"""Local, origin-deduplicated Codex usage ledger and conservative UI observations."""
import argparse
from collections import Counter, defaultdict
from datetime import datetime
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import sqlite3
import sys
import time
import uuid
import 작업_한도 as quota_adapter

TOKEN_FIELDS = ("input_tokens", "cached_input_tokens", "cache_write_input_tokens",
                "output_tokens", "reasoning_output_tokens", "total_tokens")
STAGES = ("translation", "normalization", "upload", "book_control", "shared")
DEFAULT_STATE = Path(__file__).resolve().parent / ".omc/state/measurement"
QUOTA = {"status": "unavailable", "percentPoints": None, "reason": "No validated bound-chat UI observations available. Account rate limits and token ratios are not quota attribution."}


def connection(state_dir=DEFAULT_STATE):
    state_dir = Path(state_dir)
    state_dir.mkdir(parents=True, exist_ok=True)
    db = sqlite3.connect(state_dir / "usage.sqlite3", timeout=30)
    db.row_factory = sqlite3.Row
    db.execute("PRAGMA busy_timeout=30000")
    db.executescript("""
      CREATE TABLE IF NOT EXISTS bindings (
        session_id TEXT PRIMARY KEY, book TEXT NOT NULL, stage TEXT NOT NULL,
        run_id TEXT NOT NULL, source TEXT, purpose TEXT NOT NULL, bound_at REAL NOT NULL, start_turn_id TEXT);
      CREATE TABLE IF NOT EXISTS turns (
        session_id TEXT NOT NULL, turn_id TEXT NOT NULL, book TEXT NOT NULL,
        stage TEXT NOT NULL, run_id TEXT NOT NULL, record TEXT NOT NULL,
        PRIMARY KEY(session_id, turn_id));
      CREATE TABLE IF NOT EXISTS observations (
        path TEXT PRIMARY KEY, session_id TEXT, issues TEXT NOT NULL, byte_size INTEGER, sha256 TEXT);
      CREATE TABLE IF NOT EXISTS pending (
        path TEXT PRIMARY KEY, reason TEXT NOT NULL);
    """)
    quota_adapter.initialize(db)
    return db


def timestamp(value):
    return datetime.fromisoformat(value.replace("Z", "+00:00")).timestamp()


def source_assignment(original, index, start=None, end=None):
    """Validate with the official source-index reader, preserving source bytes."""
    if not original and not index:
        if start is not None or end is not None:
            raise ValueError("Source ranges require --original and --source-index")
        return None
    if not original or not index:
        raise ValueError("--original and --source-index are required together")
    original, index = Path(original), Path(index)
    spec = importlib.util.spec_from_file_location("usage_source_converter", Path(__file__).with_name("인제스트_변환.py"))
    converter = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(converter)
    converter.parse_source_index(original, index)
    raw = original.read_bytes()
    lines = raw.decode("utf-8-sig").splitlines()
    data = json.loads(index.read_text(encoding="utf-8-sig"))
    start = 1 if start is None else start
    end = len(lines) if end is None else end
    if not 1 <= start <= end <= len(lines):
        raise ValueError("Source ranges are 1-based inclusive original lines")
    body = {}
    for chapter in data["chapters"]:
        for paragraph in chapter["paragraphs"]:
            for number in range(paragraph["start"], paragraph["end"] + 1):
                body[str(number)] = lines[number - 1]
    selected = {number: text for number, text in body.items() if start <= int(number) <= end}
    return {"sha256": hashlib.sha256(raw).hexdigest(),
            "indexSha256": hashlib.sha256(index.read_bytes()).hexdigest(),
            "original": str(original.resolve()), "lines": selected, "bodyLines": body}


def bind(db, book, stage, session_id, run_id=None, source=None, purpose="prepare", start_turn_id=None):
    book = str(Path(book).resolve())
    if not Path(book).is_dir() or stage not in STAGES or not session_id:
        raise ValueError("Existing book directory, supported stage and actual session ID required")
    with db:
        db.execute("BEGIN IMMEDIATE")
        old = db.execute("SELECT * FROM bindings WHERE session_id=?", (session_id,)).fetchone()
        if old:
            if (old["book"] != book or old["stage"] != stage
                    or old["purpose"] != purpose
                    or (start_turn_id is not None and old["start_turn_id"] != start_turn_id)
                    or (run_id and old["run_id"] != run_id)
                    or (source is not None and json.loads(old["source"] or "null") != source)):
                raise ValueError("Session assignment conflict: original book/stage/run/source is immutable")
            return dict(old)
        quota_binding = db.execute("SELECT book,run_id FROM quota_chats WHERE thread_id=?", (session_id,)).fetchone()
        if quota_binding and (quota_binding['book'] != book or (run_id and quota_binding['run_id'] != run_id)):
            raise ValueError("Session assignment conflicts with quota chat attribution")
        run_id = run_id or (quota_binding['run_id'] if quota_binding else str(uuid.uuid4()))
        db.execute("INSERT INTO bindings VALUES (?,?,?,?,?,?,?,?)",
                   (session_id, book, stage, run_id, json.dumps(source) if source else None, purpose, time.time(), start_turn_id))
    return {"session_id": session_id, "book": book, "stage": stage, "run_id": run_id}


def scan_log(path):
    """Stream a transcript; never infer a completed turn from a hook alone."""
    session_id = None
    turns, issues = [], Counter()
    current = None
    last_total = None
    model = None
    contexts = {}

    def finish(outcome, ended=None, duration=None):
        nonlocal current
        if current is None:
            return
        outcome = current.pop("_outcome", outcome)
        ended = current.pop("_ended", ended)
        duration = current.pop("_duration", duration)
        current["outcome"] = outcome
        current["endedAt"] = ended
        if duration is None and ended:
            duration = round((timestamp(ended) - timestamp(current["startedAt"])) * 1000)
        current["durationMs"] = max(0, duration) if isinstance(duration, (int, float)) else None
        current["usageStatus"] = ("counter_reset" if current.pop("reset") else
                                  "partial_tokens" if current.get("priorTokenEvidence") else
                                  "measured" if current["tokens"]["total_tokens"] is not None else "unavailable")
        current.pop("baseline")
        current.pop("latest")
        turns.append(current)
        current = None

    with Path(path).open(encoding="utf-8-sig") as stream:
        for line in stream:
            try:
                entry = json.loads(line)
                payload = entry.get("payload", {})
                if not isinstance(payload, dict):
                    raise ValueError("payload")
                kind = entry.get("type")
                if kind == "session_meta":
                    identity = payload.get("id")
                    if session_id and identity != session_id:
                        issues["conflicting_session_meta"] += 1
                        return None, [], dict(issues)
                    session_id = identity
                elif kind == "turn_context":
                    model = payload.get("model", model)
                    contexts[payload.get("turn_id")] = model
                    if current and payload.get("turn_id") == current["turnId"]:
                        current["model"] = model
                elif kind == "event_msg":
                    event = payload.get("type")
                    if event == "task_started":
                        started = entry["timestamp"]
                        timestamp(started)
                        recorded = payload.get("started_at")
                        if recorded is not None and (not isinstance(recorded, (int, float)) or abs(timestamp(started) - recorded) > 2):
                            issues["mirrored_task_started"] += 1
                            continue
                        if not isinstance(payload.get("turn_id"), str) or not payload["turn_id"]:
                            issues["missing_turn_id"] += 1
                            continue
                        finish("partial")
                        current = {"turnId": payload["turn_id"], "startedAt": started,
                                   "model": contexts.get(payload["turn_id"], model),
                                   "tokens": dict.fromkeys(TOKEN_FIELDS), "baseline": last_total,
                                   "latest": None, "reset": False}
                    elif event == "token_count":
                        info = payload.get("info") or {}
                        total = info.get("total_token_usage")
                        if not isinstance(total, dict):
                            continue
                        total = {k: v for k, v in total.items() if k in TOKEN_FIELDS and type(v) is int and v >= 0}
                        if current:
                            if current["baseline"] is None:
                                last = info.get("last_token_usage") or {}
                                current["baseline"] = {k: v - last[k] for k, v in total.items()
                                                       if type(last.get(k)) is int and 0 <= last[k] <= v}
                            previous = current["latest"] or current["baseline"]
                            if any(total[k] < previous[k] for k in total.keys() & previous.keys()):
                                current["reset"] = True
                            current["latest"] = total
                            previous_tokens = current["tokens"]
                            if not current["reset"]:
                                current["tokens"] = {k: total[k] - current["baseline"][k]
                                                     if k in total and k in current["baseline"] else None for k in TOKEN_FIELDS}
                            else:
                                current["tokens"] = dict.fromkeys(TOKEN_FIELDS)
                            evidence = dict(current.get("priorTokenEvidence", {}))
                            for key, value in current["tokens"].items():
                                if value is not None:
                                    evidence.pop(key, None)
                                elif previous_tokens[key] is not None:
                                    evidence[key] = previous_tokens[key]
                            if evidence:
                                current["priorTokenEvidence"] = evidence
                            else:
                                current.pop("priorTokenEvidence", None)
                        last_total = total
                    elif event in ("task_complete", "turn_aborted"):
                        if current and payload.get("turn_id") == current["turnId"]:
                            timestamp(entry["timestamp"])
                            current.update(_outcome="completed" if event == "task_complete" else "aborted",
                                           _ended=entry["timestamp"], _duration=payload.get("duration_ms"))
            except (ValueError, TypeError, KeyError, AttributeError):
                issues["invalid_records"] += 1
    finish("pending")
    if not session_id:
        issues["missing_session_meta"] += 1
    for turn in turns:
        turn["sessionId"] = session_id
    return session_id, turns, dict(issues)


def store_turn(db, binding, record, appended=False):
    key = (record["sessionId"], record["turnId"])
    old = db.execute("SELECT * FROM turns WHERE session_id=? AND turn_id=?", key).fetchone()
    if old:
        if (old["book"], old["stage"], old["run_id"]) != (binding["book"], binding["stage"], binding["run_id"]):
            raise ValueError("Original session/turn attribution conflict")
        prior = json.loads(old["record"])
        retained = {key: value for key, value in prior.get("priorTokenEvidence", {}).items()
                    if record["tokens"][key] is None}
        if retained:
            record["priorTokenEvidence"] = {**retained, **record.get("priorTokenEvidence", {})}
            record["usageStatus"] = "partial_tokens"
        if prior == record:
            return "duplicate"
        # Only delayed completion may upgrade an unfinished original record.
        if prior["outcome"] in ("completed", "aborted"):
            if record["outcome"] in ("pending", "partial"):
                return "stale_copy"
            stable = ("startedAt", "endedAt", "durationMs", "outcome", "model")
            if not appended or any(prior[key] != record[key] for key in stable):
                raise ValueError("Conflicting copies of an original completed turn")
            if any(record["tokens"][key] is not None and value is not None and record["tokens"][key] < value
                   for key, value in prior["tokens"].items()):
                raise ValueError("Appended completed usage counters decreased")
            missing = {key: value for key, value in prior["tokens"].items()
                       if value is not None and record["tokens"][key] is None}
            if missing:
                record["priorTokenEvidence"] = {**prior.get("priorTokenEvidence", {}), **missing}
                record["usageStatus"] = "partial_tokens"
        if prior["startedAt"] != record["startedAt"]:
            raise ValueError("Conflicting original turn start")
        old_total, new_total = prior["tokens"]["total_tokens"], record["tokens"]["total_tokens"]
        if record["outcome"] in ("pending", "partial"):
            if old_total is not None and ((new_total is not None and new_total < old_total)
                                         or record["usageStatus"] == "unavailable"):
                return "stale_copy"
            if prior["outcome"] == "partial" and record["outcome"] == "pending" and old_total == new_total:
                return "stale_copy"
    db.execute("INSERT OR REPLACE INTO turns VALUES (?,?,?,?,?,?)", (*key, binding["book"], binding["stage"], binding["run_id"], json.dumps(record, sort_keys=True)))
    return "updated" if old else "inserted"


def file_identity(path, limit=None):
    digest, size = hashlib.sha256(), 0
    with Path(path).open("rb") as stream:
        while limit is None or size < limit:
            block = stream.read(65536 if limit is None else min(65536, limit - size))
            if not block:
                break
            digest.update(block)
            size += len(block)
    return size, digest.hexdigest()


def reconcile_file(db, path):
    path = str(Path(path).resolve())
    previous = db.execute("SELECT * FROM observations WHERE path=?", (path,)).fetchone()
    byte_size, digest = file_identity(path)
    appended = bool(previous and previous["byte_size"] is not None and byte_size > previous["byte_size"]
                    and file_identity(path, previous["byte_size"])[1] == previous["sha256"])
    session_id, turns, issues = scan_log(path)
    counts = Counter()
    with db:
        binding = db.execute("SELECT * FROM bindings WHERE session_id=?", (session_id,)).fetchone()
        db.execute("INSERT OR REPLACE INTO observations VALUES (?,?,?,?,?)", (path, session_id, json.dumps(issues), byte_size, digest))
        if not binding:
            db.execute("INSERT OR REPLACE INTO pending VALUES (?,?)", (path, "unmapped_actual_session" if session_id else "missing_session_meta"))
            return {"pending": 1, "issues": issues}
        db.execute("DELETE FROM pending WHERE path=?", (path,))
        boundary = binding["start_turn_id"]
        started = boundary is None
        for turn in turns:
            if turn["turnId"] == boundary:
                started = True
            eligible = started and (boundary is not None or timestamp(turn["startedAt"]) >= binding["bound_at"])
            if eligible:
                counts[store_turn(db, binding, turn, appended)] += 1
            else:
                issues["before_assignment_boundary"] = issues.get("before_assignment_boundary", 0) + 1
        if boundary is not None and not started:
            issues["assignment_boundary_not_found"] = 1
        db.execute("UPDATE observations SET issues=? WHERE path=?", (json.dumps(issues), path))
    persist_report(db, binding["book"])
    return {**counts, "issues": issues}


def discover_session(path):
    """Inspect metadata only before deciding whether an account log is ours."""
    with Path(path).open(encoding="utf-8-sig") as stream:
        for line in stream:
            try:
                entry = json.loads(line)
                if entry.get("type") == "session_meta":
                    payload = entry.get("payload")
                    return payload.get("id") if isinstance(payload, dict) else None
            except (ValueError, AttributeError):
                continue
    return None


def reconcile(db, logs_root=None):
    root = Path(logs_root) if logs_root else Path(os.environ.get("CODEX_HOME", str(Path.home() / ".codex"))) / "sessions"
    paths = set(root.rglob("*.jsonl")) if root.exists() else set()
    paths.update(Path(row[0]) for row in db.execute("SELECT path FROM pending") if Path(row[0]).is_file())
    results = Counter()
    bound = {row[0] for row in db.execute("SELECT session_id FROM bindings")}
    for path in sorted(paths):
        try:
            if discover_session(path) not in bound:
                continue
            result = reconcile_file(db, path)
            results.update({k: v for k, v in result.items() if k != "issues"})
        except (OSError, ValueError) as error:
            with db:
                db.execute("INSERT OR REPLACE INTO pending VALUES (?,?)", (str(path.resolve()), str(error)))
            results["failed_logs"] += 1
    if not root.exists():
        results["logs_root_missing"] += 1
    return dict(results)


def interval_union(intervals):
    merged = []
    for start, end in sorted(intervals):
        if merged and start <= merged[-1][1]:
            merged[-1][1] = max(merged[-1][1], end)
        else:
            merged.append([start, end])
    return round(sum(end - start for start, end in merged) * 1000)


def summarize(records):
    totals = {key: 0 for key in TOKEN_FIELDS}
    known = Counter()
    models, outcomes, statuses = Counter(), Counter(), Counter()
    by_model = {}
    intervals = []
    for item in records:
        actual_model = item["model"] or "unknown"
        models[actual_model] += 1
        model_tokens = by_model.setdefault(actual_model, dict.fromkeys(TOKEN_FIELDS))
        outcomes[item["outcome"]] += 1
        statuses[item["usageStatus"]] += 1
        for key, value in item["tokens"].items():
            if value is not None:
                totals[key] += value
                model_tokens[key] = (model_tokens[key] or 0) + value
                known[key] += 1
        if item["endedAt"]:
            intervals.append((timestamp(item["startedAt"]), timestamp(item["endedAt"])))
    return {"turns": len(records), "tokens": {key: value if known[key] else None for key, value in totals.items()}, "tokenMeasuredTurns": dict(known),
            "models": dict(models), "tokensByModel": by_model, "outcomes": dict(outcomes), "usageStatuses": dict(statuses),
            "summedDurationMs": sum(r["durationMs"] or 0 for r in records),
            "intervalUnionMs": interval_union(intervals),
            "calendarDurationMs": round((max(end for _, end in intervals) - min(start for start, _ in intervals)) * 1000) if intervals else None}


def report(db, book, run_id=None):
    book = str(Path(book).resolve())
    rows = db.execute("SELECT * FROM turns WHERE book=? AND (? IS NULL OR run_id=?)", (book, run_id, run_id)).fetchall()
    groups = defaultdict(list)
    for row in rows:
        groups[row["stage"]].append(json.loads(row["record"]))
    bindings = db.execute("SELECT * FROM bindings WHERE book=? AND (? IS NULL OR run_id=?)", (book, run_id, run_id)).fetchall()
    body, selected, drafts = defaultdict(dict), defaultdict(dict), defaultdict(dict)
    for binding in bindings:
        if binding["source"]:
            source = json.loads(binding["source"])
            body[source["sha256"]].update(source["bodyLines"])
            selected[source["sha256"]].update(source["lines"])
            if binding["purpose"] == "draft":
                drafts[source["sha256"]].update(source["lines"])
    issues = Counter()
    sessions = {b["session_id"] for b in bindings}
    observed = set()
    for row in db.execute("SELECT * FROM observations"):
        if row["session_id"] in sessions:
            observed.add(row["session_id"])
            issues.update(json.loads(row["issues"]))
    missing = sorted(sessions - observed)
    if not sessions:
        issues["no_bound_sessions"] += 1
    pending = [dict(row) for row in db.execute("SELECT pending.* FROM pending JOIN observations USING(path) WHERE observations.session_id IN (SELECT session_id FROM bindings WHERE book=? AND (? IS NULL OR run_id=?))", (book, run_id, run_id))]
    unmapped = db.execute("SELECT count(*) FROM pending LEFT JOIN observations USING(path) WHERE observations.session_id IS NULL OR observations.session_id NOT IN (SELECT session_id FROM bindings)").fetchone()[0]
    records = [json.loads(row["record"]) for row in rows if row["stage"] not in ("book_control", "shared")]
    uncertain = sum(item["outcome"] in ("pending", "partial") or item["usageStatus"] != "measured" for item in [json.loads(row["record"]) for row in rows if row["stage"] != "shared"])
    return {"book": book, "run_id": run_id, "production": summarize(records),
            "bookTotal": summarize([json.loads(row["record"]) for row in rows if row["stage"] != "shared"]),
            "bindings": [{k: binding[k] for k in ("session_id", "stage", "run_id", "purpose", "bound_at", "start_turn_id")} for binding in bindings],
            "stages": {stage: summarize(items) for stage, items in groups.items()},
            "book_control": summarize(groups["book_control"]), "shared": summarize(groups["shared"]),
            "sourceVolume": {"scope": "assigned_indexed_body", "status": "available" if body else "unavailable",
                             "characters": sum(len("".join(text.split())) for lines in selected.values() for text in lines.values()),
                             "declaredDraftCharacters": sum(len("".join(text.split())) for lines in drafts.values() for text in lines.values()),
                             "draftStatus": "declared_scope_only_no_artifact_approval",
                             "indexedBodyCharacters": sum(len("".join(text.split())) for lines in body.values() for text in lines.values()),
                             "coveredLines": sum(len(lines) for lines in selected.values()),
                             "sourceHashes": sorted(body), "reason": "Declared assignment scope; not proof of completed translation. Characters exclude all Unicode whitespace."},
            "coverage": {"status": "incomplete" if missing or uncertain or issues or pending else "observed_only",
                         "boundSessions": len(sessions), "missingSessionLogs": missing,
                         "uncertainBookTurns": uncertain,
                         "uncertainProductionTurns": sum(item["outcome"] in ("pending", "partial") or item["usageStatus"] != "measured" for item in records), "issues": dict(issues), "pendingLogs": pending,
                         "reason": "Only explicitly bound actual sessions are attributable. Unbound children, absent external/image-tool logs and pre-init work cannot be inferred."},
            "registryUnmappedPendingLogs": unmapped,
            "turnEvidenceGaps": [{"sessionId": item["sessionId"], "turnId": item["turnId"],
                                  "priorMeasuredTokenLowerBounds": item["priorTokenEvidence"],
                                  "reason": "Appended usage omitted previously measured fields; current exact values are unknown."}
                                 for item in [json.loads(row["record"]) for row in rows] if item.get("priorTokenEvidence")],
            "quota": quota_adapter.quota_report(db, book, run_id)}


def persist_report(db, book):
    """Atomically refresh local summaries; these are never ingest/ZIP artifacts."""
    # Serialize summary replacement with other collectors on Windows.
    with db:
        db.execute("BEGIN IMMEDIATE")
        state_dir = Path(db.execute("PRAGMA database_list").fetchone()[2]).parent
        destination = state_dir / "reports" / hashlib.sha256(str(Path(book).resolve()).encode()).hexdigest()[:16]
        destination.mkdir(parents=True, exist_ok=True)
        data = report(db, book)
        total = data["bookTotal"]
        quota = data["quota"]
        quota_text = f"Weekly book quota: unconfirmed. {quota['reason']}"
        for observation in quota["chatObservations"]:
            quota_text += (f"\nObserved chat display: {observation['displayedWeeklyPercentText']}"
                           f" at {observation['observedAt']}; reporting period, freshness and child coverage unverified.")
        text = (f"# Usage: {Path(book).name}\n\n"
                f"Measured tokens: {total['tokens']['total_tokens']}.\n"
                f"Summed duration: {total['summedDurationMs']} ms; interval union: {total['intervalUnionMs']} ms.\n"
                f"Assigned indexed source characters: {data['sourceVolume']['characters']} (declared scope, not completed translation).\n"
                f"Coverage: {data['coverage']['status']}.\n"
                f"{quota_text}\n\n"
                "Token sums include measured fields only. See report.json for missing data, models, control stages and declared source scopes.\n")
        for name, content in (("report.json", json.dumps(data, ensure_ascii=False, indent=2) + "\n"), ("report.md", text)):
            temporary = destination / (name + "." + uuid.uuid4().hex + ".tmp")
            try:
                temporary.write_text(content, encoding="utf-8")
                os.replace(temporary, destination / name)
            finally:
                temporary.unlink(missing_ok=True)
        return str(destination)



def read_turn(log_path, turn_id):
    _, records, issues = scan_log(log_path)
    for record in records:
        if record["turnId"] == turn_id and record["outcome"] in ("completed", "aborted"):
            return record
    raise ValueError("No real completed/aborted original turn found: " + json.dumps(issues))


def main(argv=None):
    argv = list(sys.argv[1:] if argv is None else argv)
    commands = ("init", "bind", "reconcile", "report", "hook", "probe", "quota-bind", "quota-import")
    legacy = not any(arg in commands for arg in argv) and "--log" in argv
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=commands, nargs="?", default="reconcile")
    parser.add_argument("--state-dir", type=Path, default=DEFAULT_STATE)
    parser.add_argument("--book", type=Path)
    parser.add_argument("--stage", choices=STAGES)
    parser.add_argument("--session-id")
    parser.add_argument("--run-id")
    parser.add_argument("--chat-title")
    parser.add_argument("--snapshot", type=Path)
    parser.add_argument("--purpose", choices=("draft", "review", "correction", "prepare", "comparison"), default="prepare")
    parser.add_argument("--turn-id")
    parser.add_argument("--log", type=Path)
    parser.add_argument("--logs-root", type=Path)
    parser.add_argument("--original", type=Path)
    parser.add_argument("--source-index", type=Path)
    parser.add_argument("--range-start", type=int)
    parser.add_argument("--range-end", type=int)
    args = parser.parse_args(argv)
    try:
        if args.command == "probe" and not legacy:
            print(json.dumps({"quota": QUOTA, "quotaObservationSource": "codex_desktop_accessibility",
                              "tokens": "local_session_logs", "provider": "codex"}))
            return 0
        db = connection(args.state_dir)
        try:
            if legacy or args.command in ("init", "bind"):
                if not args.book or not args.stage or not args.session_id:
                    raise ValueError("--book, --stage and --session-id required")
                if legacy:
                    if not args.log or not args.turn_id:
                        raise ValueError("Legacy CLI requires --log and --turn-id")
                    if any(c not in "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-" for c in args.turn_id):
                        raise ValueError("Invalid turn ID")
                    legacy_record = read_turn(args.log, args.turn_id)
                    if legacy_record["sessionId"] != args.session_id:
                        raise ValueError("Actual session_meta ID differs from --session-id")
                source = source_assignment(args.original, args.source_index, args.range_start, args.range_end)
                boundary = args.turn_id
                if legacy and db.execute("SELECT 1 FROM bindings WHERE session_id=?", (args.session_id,)).fetchone():
                    boundary = None
                result = bind(db, args.book, args.stage, args.session_id, args.run_id, source, args.purpose, boundary)
                persist_report(db, args.book)
                if legacy:
                    record = legacy_record
                    with db:
                        store_turn(db, result, record)
                    persist_report(db, args.book)
                    record.update(stage=args.stage, sourceLog=args.log.name)
                    path = args.book / "작업" / "사용량" / f"{args.turn_id}.json"
                    path.parent.mkdir(parents=True, exist_ok=True)
                    if path.exists() and json.loads(path.read_text(encoding="utf-8")) != record:
                        raise ValueError("Existing legacy record differs")
                    if not path.exists():
                        with path.open("x", encoding="utf-8") as output:
                            json.dump(record, output, ensure_ascii=False, indent=2)
                            output.write("\n")
                    result = {"path": str(path)}
            elif args.command in ("quota-bind", "quota-import"):
                if not args.book or not args.run_id or not args.session_id:
                    raise ValueError("Quota commands require --book, --run-id and --session-id")
                if args.command == "quota-bind":
                    result = quota_adapter.bind_chat(db, args.book, args.run_id, args.session_id, args.chat_title)
                else:
                    if not args.snapshot:
                        raise ValueError("quota-import requires --snapshot")
                    result = quota_adapter.import_snapshot(db, args.book, args.run_id, args.session_id,
                                                           json.loads(args.snapshot.read_text(encoding="utf-8-sig")))
                persist_report(db, args.book)
            elif args.command == "report":
                if not args.book:
                    raise ValueError("report requires --book")
                result = report(db, args.book, args.run_id)
            elif args.command == "hook":
                event = json.load(sys.stdin)
                if not isinstance(event, dict):
                    raise ValueError("Hook JSON must be an object")
                transcript = event.get("transcript_path")
                # Hook session_id can be the parent. The transcript's session_meta wins.
                if transcript and Path(transcript).is_file():
                    result = reconcile_file(db, transcript)
                else:
                    marker = str(Path(transcript).resolve()) if transcript else "hook_without_transcript"
                    with db:
                        db.execute("INSERT OR REPLACE INTO pending VALUES (?,?)", (marker, "hook_without_readable_transcript"))
                    result = {"pending": 1}
            else:
                result = reconcile(db, args.logs_root)
        finally:
            db.close()
        # Hooks must not inject text or prompts into Codex's conversation.
        if args.command != "hook":
            print(json.dumps(result, ensure_ascii=False, indent=2))
        return 0
    except (OSError, ValueError, TypeError, sqlite3.Error) as error:
        if args.command == "hook":
            print("Usage hook deferred: " + str(error), file=sys.stderr)
            return 0
        parser.error(str(error))


if __name__ == "__main__":
    raise SystemExit(main())
