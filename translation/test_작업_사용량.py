"""Origin identity, delayed event and conservative usage regression tests."""
from concurrent.futures import ThreadPoolExecutor
import contextlib
import hashlib
import importlib.util
import io
import json
from pathlib import Path
import tempfile
import threading
import unittest
from unittest.mock import patch

SPEC = importlib.util.spec_from_file_location("usage", Path(__file__).with_name("작업_사용량.py"))
usage = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(usage)


class UsageTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.book = self.root / "book"
        self.book.mkdir()
        self.state = self.root / "state"
        self.db = usage.connection(self.state)
        self.addCleanup(self.db.close)

    def bind(self, *args, **kwargs):
        kwargs.setdefault("start_turn_id", "t")
        return usage.bind(self.db, *args, **kwargs)

    def events(self, session="s", turn="t", complete=True, start=0):
        def row(kind, payload, seconds=0):
            return {"type": kind, "timestamp": f"2026-11-18T00:00:{seconds:02d}Z", "payload": payload}
        items = [row("session_meta", {"id": session}),
                 row("turn_context", {"turn_id": turn, "model": "actual-model"}),
                 row("event_msg", {"type": "task_started", "turn_id": turn}, start),
                 row("event_msg", {"type": "token_count", "info": {
                     "total_token_usage": {"input_tokens": 120, "cached_input_tokens": 30, "output_tokens": 40, "total_tokens": 160},
                     "last_token_usage": {"input_tokens": 20, "cached_input_tokens": 5, "output_tokens": 10, "total_tokens": 30}}}, start + 1)]
        if complete:
            items.append(row("event_msg", {"type": "task_complete", "turn_id": turn}, start + 5))
        return items

    def log(self, name="log.jsonl", **kwargs):
        path = self.root / name
        path.write_text("".join(json.dumps(item) + "\n" for item in self.events(**kwargs)), encoding="utf-8")
        return path

    def test_automatic_summary_persistence(self):
        self.bind( self.book, "translation", "s")
        usage.reconcile_file(self.db, self.log())
        reports = list((self.state / "reports").glob("*/report.json"))
        self.assertEqual(len(reports), 1)
        data = json.loads(reports[0].read_text(encoding="utf-8"))
        self.assertEqual(data["bookTotal"]["turns"], 1)
        self.assertIn("Weekly book quota: unconfirmed", reports[0].with_suffix(".md").read_text(encoding="utf-8"))
        self.assertNotIn(" turns", reports[0].with_suffix(".md").read_text(encoding="utf-8"))
        self.assertEqual(list((self.state / "reports").rglob("*.tmp")), [])

    def test_quota_binding_and_token_ledger_agree_in_both_orders(self):
        usage.quota_adapter.bind_chat(self.db, self.book, "run", "chat", "Book chat")
        with self.assertRaisesRegex(ValueError, "conflicts"):
            usage.bind(self.db, self.book, "translation", "chat", "different")
        self.assertEqual(usage.bind(self.db, self.book, "translation", "chat")["run_id"], "run")
        usage.bind(self.db, self.book, "translation", "second", "run")
        with self.assertRaisesRegex(ValueError, "conflicts"):
            usage.quota_adapter.bind_chat(self.db, self.book, "different", "second", "Second chat")

    def test_quota_cli_import_refreshes_sanitized_book_report(self):
        from test_작업_한도 import AX
        snapshot = self.root / "snapshot.json"
        snapshot.write_text(json.dumps({"source": "codex_desktop_accessibility", "accessibilityText": AX,
                                        "observedAt": "2026-11-18T05:24:38Z"}), encoding="utf-8")
        common = ["--state-dir", str(self.state), "--book", str(self.book), "--run-id", "run", "--session-id", "chat"]
        with contextlib.redirect_stdout(io.StringIO()):
            self.assertEqual(usage.main(["quota-bind", *common, "--chat-title", "Dedicated book fixture"]), 0)
            self.assertEqual(usage.main(["quota-import", *common, "--snapshot", str(snapshot)]), 0)
        path = next((self.state / "reports").glob("*/report.json"))
        data = json.loads(path.read_text(encoding="utf-8"))
        self.assertEqual(data["quota"]["displayedWeeklyPercent"], 35.32)
        self.assertIsNone(data["quota"]["percentPoints"])
        self.assertNotIn("accessibilityText", path.read_text(encoding="utf-8"))
        human = path.with_suffix(".md").read_text(encoding="utf-8")
        self.assertIn("35.32%", human)
        self.assertIn("unconfirmed", human)
        self.assertNotIn("observationCount", human)

    def test_concurrent_quota_and_token_assignment_have_one_owner(self):
        barrier = threading.Barrier(2)

        def assign(kind):
            db = usage.connection(self.state)
            try:
                barrier.wait(timeout=10)
                try:
                    if kind == "quota":
                        usage.quota_adapter.bind_chat(db, self.book, "quota-run", "race", "Race fixture")
                    else:
                        usage.bind(db, self.book, "translation", "race", "token-run")
                    return "bound"
                except ValueError:
                    return "conflict"
            finally:
                db.close()

        with ThreadPoolExecutor(max_workers=2) as pool:
            self.assertEqual(sorted(pool.map(assign, ("quota", "token"))), ["bound", "conflict"])

    def test_global_origin_idempotence_and_stage_conflict(self):
        self.bind( self.book, "translation", "s")
        path = self.log()
        usage.reconcile_file(self.db, path)
        copy = self.root / "other-host.jsonl"
        copy.write_bytes(path.read_bytes())
        self.assertEqual(usage.reconcile_file(self.db, copy)["duplicate"], 1)
        result = usage.report(self.db, self.book)
        self.assertEqual(result["production"]["tokens"]["total_tokens"], 30)
        self.assertEqual(result["production"]["tokensByModel"]["actual-model"]["input_tokens"], 20)
        with self.assertRaisesRegex(ValueError, "conflict"):
            self.bind( self.book, "upload", "s")
        other = self.root / "other"
        other.mkdir()
        with self.assertRaises(ValueError):
            self.bind( other, "translation", "s")

    def test_pending_unmapped_delayed_complete_and_partial(self):
        path = self.log(complete=False)
        self.assertEqual(usage.reconcile_file(self.db, path)["pending"], 1)
        self.bind( self.book, "translation", "s")
        usage.reconcile(self.db, self.root)
        self.assertEqual(usage.report(self.db, self.book)["production"]["outcomes"], {"pending": 1})
        path = self.log()
        usage.reconcile_file(self.db, path)
        self.assertEqual(usage.report(self.db, self.book)["production"]["outcomes"], {"completed": 1})
        records = self.events(complete=False) + self.events(turn="next", start=10)[2:]
        path.write_text("\n".join(map(json.dumps, records)), encoding="utf-8")
        _, turns, _ = usage.scan_log(path)
        self.assertEqual([r["outcome"] for r in turns], ["partial", "completed"])

    def test_hook_reads_actual_child_not_parent_and_is_silent(self):
        self.bind( self.book, "book_control", "parent")
        self.bind( self.book, "translation", "child")
        path = self.log(session="child")
        output = io.StringIO()
        with patch("sys.stdin", io.StringIO(json.dumps({"session_id": "parent", "transcript_path": str(path)}))), contextlib.redirect_stdout(output):
            self.assertEqual(usage.main(["hook", "--state-dir", str(self.state)]), 0)
        self.assertEqual(output.getvalue(), "")
        self.assertEqual(self.db.execute("SELECT session_id FROM turns").fetchone()[0], "child")

    def test_concurrent_same_origin(self):
        self.bind( self.book, "translation", "s")
        path = self.log()
        def collect(_):
            db = usage.connection(self.state)
            try:
                return usage.reconcile_file(db, path)
            finally:
                db.close()
        with ThreadPoolExecutor(max_workers=5) as pool:
            results = list(pool.map(collect, range(10)))
        self.assertEqual(self.db.execute("SELECT count(*) FROM turns").fetchone()[0], 1)
        self.assertEqual(sum(result.get("inserted", 0) for result in results), 1)

    def test_reconcile_ignores_unrelated_account_logs(self):
        self.bind( self.book, "translation", "s")
        self.log("arbitrary-host-copy-name.jsonl")
        self.log("unrelated.jsonl", session="unrelated")
        usage.reconcile(self.db, self.root)
        self.assertEqual(self.db.execute("SELECT count(*) FROM observations").fetchone()[0], 1)
        self.assertEqual(self.db.execute("SELECT count(*) FROM pending").fetchone()[0], 0)

    def test_first_scan_sparse_usage_preserves_evidence_for_pending_and_complete(self):
        self.bind(self.book, "translation", "s")
        path = self.log(complete=False)
        sparse = {"type": "event_msg", "payload": {"type": "token_count", "info": {"total_token_usage": {"total_tokens": 200}}}}
        with path.open("a", encoding="utf-8") as stream:
            stream.write(json.dumps(sparse) + "\n")
        usage.reconcile_file(self.db, path)
        data = usage.report(self.db, self.book)
        self.assertEqual(data["production"]["outcomes"], {"pending": 1})
        self.assertEqual(data["production"]["usageStatuses"], {"partial_tokens": 1})
        self.assertEqual(data["turnEvidenceGaps"][0]["priorMeasuredTokenLowerBounds"]["input_tokens"], 20)
        self.assertEqual(data["turnEvidenceGaps"][0]["priorMeasuredTokenLowerBounds"]["output_tokens"], 10)
        self.assertIsNone(data["production"]["tokens"]["input_tokens"])
        # The same sparse history scanned for the first time with its end already
        # present must retain evidence without needing any earlier DB record.
        with path.open("a", encoding="utf-8") as stream:
            stream.write(json.dumps(self.events()[-1]) + "\n")
        record = usage.read_turn(path, "t")
        self.assertEqual(record["usageStatus"], "partial_tokens")
        self.assertEqual(record["priorTokenEvidence"]["input_tokens"], 20)
        usage.reconcile_file(self.db, path)
        self.assertEqual(usage.report(self.db, self.book)["production"]["outcomes"], {"completed": 1})

    def test_completed_usage_can_enrich_only_from_verified_append(self):
        self.bind(self.book, "translation", "s")
        path = self.log()
        usage.reconcile_file(self.db, path)
        late = {"type": "event_msg", "payload": {"type": "token_count", "info": {"total_token_usage": {"total_tokens": 200}}}}
        with path.open("a", encoding="utf-8") as stream:
            stream.write(json.dumps(late) + "\n")
        usage.reconcile_file(self.db, path)
        self.assertEqual(usage.report(self.db, self.book)["production"]["tokens"]["total_tokens"], 70)
        enriched = usage.report(self.db, self.book)
        self.assertIsNone(enriched["production"]["tokens"]["input_tokens"])
        self.assertEqual(enriched["turnEvidenceGaps"][0]["priorMeasuredTokenLowerBounds"]["input_tokens"], 20)
        self.assertEqual(enriched["production"]["usageStatuses"], {"partial_tokens": 1})
        self.assertEqual(usage.reconcile_file(self.db, path)["duplicate"], 1)
        conflict = self.root / "conflict.jsonl"
        conflict.write_text(path.read_text(encoding="utf-8").replace('200', '210'), encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "Conflicting copies"):
            usage.reconcile_file(self.db, conflict)

    def test_binding_boundary_and_run_filter(self):
        # No explicit boundary means old session history is excluded.
        usage.bind(self.db, self.book, "translation", "old", run_id="oldrun")
        usage.reconcile_file(self.db, self.log("old.jsonl", session="old"))
        self.assertEqual(usage.report(self.db, self.book)["production"]["turns"], 0)
        usage.bind(self.db, self.book, "translation", "new", run_id="newrun", start_turn_id="second")
        path = self.log("new.jsonl", session="new")
        with path.open("a", encoding="utf-8") as stream:
            stream.write("\n".join(map(json.dumps, self.events(session="new", turn="second", start=10)[1:])))
        usage.reconcile_file(self.db, path)
        data = usage.report(self.db, self.book, "newrun")
        self.assertEqual(data["production"]["turns"], 1)
        self.assertEqual(data["coverage"]["boundSessions"], 1)
        self.assertEqual(data["bindings"][0]["session_id"], "new")

    def test_stale_pending_never_reduces_tokens(self):
        self.bind( self.book, "translation", "s")
        path = self.log(complete=False)
        rich = self.events(complete=False)
        rich.append({"type": "event_msg", "payload": {"type": "token_count", "info": {"total_token_usage": {"total_tokens": 200}}}})
        path.write_text("\n".join(map(json.dumps, rich)), encoding="utf-8")
        usage.reconcile_file(self.db, path)
        usage.reconcile_file(self.db, self.log("stale.jsonl", complete=False))
        self.assertEqual(usage.report(self.db, self.book)["production"]["tokens"]["total_tokens"], 70)

    def test_late_usage_after_complete_and_rejected_mirror(self):
        path = self.log()
        events = self.events()
        late = {"type": "event_msg", "payload": {"type": "token_count", "info": {"total_token_usage": {"total_tokens": 200}, "last_token_usage": {"total_tokens": 70}}}}
        events.append(late)
        path.write_text("\n".join(map(json.dumps, events)), encoding="utf-8")
        self.assertEqual(usage.read_turn(path, "t")["tokens"]["total_tokens"], 70)
        # A first token observation can itself be delayed until after completion.
        del events[3]
        path.write_text("\n".join(map(json.dumps, events)), encoding="utf-8")
        self.assertEqual(usage.read_turn(path, "t")["tokens"]["total_tokens"], 70)
        events.insert(3, {"type": "event_msg", "timestamp": "2026-11-18T00:00:02Z", "payload": {"type": "task_started", "turn_id": "mirror", "started_at": 1}})
        path.write_text("\n".join(map(json.dumps, events)), encoding="utf-8")
        self.assertEqual(usage.read_turn(path, "t")["outcome"], "completed")

    def test_unknown_tokens_are_null_and_unmapped_does_not_contaminate(self):
        self.bind( self.book, "translation", "s")
        usage.reconcile_file(self.db, self.log("unmapped.jsonl", session="stranger"))
        data = usage.report(self.db, self.book)
        self.assertIsNone(data["production"]["tokens"]["total_tokens"])
        self.assertEqual(data["coverage"]["pendingLogs"], [])
        self.assertEqual(data["registryUnmappedPendingLogs"], 1)

    def test_counter_reset_and_invalid_mirror(self):
        path = self.log()
        items = self.events()
        items.insert(-1, {"type": "event_msg", "payload": {"type": "token_count", "info": {"total_token_usage": {"total_tokens": 2}}}})
        items.insert(1, {"type": "event_msg", "timestamp": "2026-11-18T00:00:00Z", "payload": {"type": "task_started", "turn_id": "mirror", "started_at": 1}})
        path.write_text("invalid\n" + "\n".join(map(json.dumps, items)), encoding="utf-8")
        _, turns, issues = usage.scan_log(path)
        self.assertEqual(len(turns), 1)
        self.assertEqual(turns[0]["usageStatus"], "counter_reset")
        self.assertIsNone(turns[0]["tokens"]["total_tokens"])
        self.assertEqual(issues, {"invalid_records": 1, "mirrored_task_started": 1})

    def test_time_union_and_control_separation(self):
        for session, stage in [("a", "translation"), ("b", "translation"), ("c", "book_control")]:
            self.bind( self.book, stage, session)
            usage.reconcile_file(self.db, self.log(session + ".jsonl", session=session))
        report = usage.report(self.db, self.book)
        self.assertEqual(report["production"]["summedDurationMs"], 10000)
        self.assertEqual(report["production"]["intervalUnionMs"], 5000)
        self.assertEqual(report["bookTotal"]["tokens"]["total_tokens"], 90)
        self.assertEqual(report["book_control"]["tokens"]["total_tokens"], 30)
        self.assertEqual(report["quota"]["status"], "unavailable")
        self.assertIsNone(report["quota"]["percentPoints"])

    def test_legacy_idempotence(self):
        path = self.log()
        arguments = ["--book", str(self.book), "--stage", "translation", "--session-id", "s", "--turn-id", "t", "--log", str(path), "--state-dir", str(self.state)]
        with contextlib.redirect_stdout(io.StringIO()):
            usage.main(arguments)
            usage.main(arguments)
        record = json.loads((self.book / "작업/사용량/t.json").read_text(encoding="utf-8"))
        self.assertEqual(record["sessionId"], "s")
        self.assertEqual(self.db.execute("SELECT count(*) FROM turns").fetchone()[0], 1)
        with path.open("a", encoding="utf-8") as stream:
            stream.write("\n".join(map(json.dumps, self.events(turn="next", start=10)[1:])))
        arguments[arguments.index("--turn-id") + 1] = "next"
        with contextlib.redirect_stdout(io.StringIO()):
            usage.main(arguments)
        self.assertEqual(self.db.execute("SELECT count(*) FROM turns").fetchone()[0], 2)

    def test_official_source_hash_range_union_and_purpose(self):
        original = self.root / "original.txt"
        original.write_bytes("one \n two\t\nthree\u2003\n".encode())
        index = self.root / "index.json"
        index.write_text(json.dumps({"version": 1, "sourceSha256": hashlib.sha256(original.read_bytes()).hexdigest(), "chapters": [{"title": "I", "paragraphs": [{"start": 1, "end": 3}]}], "exclusions": []}), encoding="utf-8")
        first = usage.source_assignment(original, index, 1, 2)
        second = usage.source_assignment(original, index, 2, 3)
        self.bind( self.book, "translation", "a", source=first, purpose="draft")
        self.bind( self.book, "translation", "b", source=second, purpose="draft")
        self.bind( self.book, "translation", "c", source=second, purpose="correction")
        volume = usage.report(self.db, self.book)["sourceVolume"]
        self.assertEqual(volume["coveredLines"], 3)
        self.assertEqual(volume["characters"], 11)
        self.assertEqual(volume["declaredDraftCharacters"], 11)
        original.write_bytes(original.read_bytes() + b"changed")
        with self.assertRaises(ValueError):
            usage.source_assignment(original, index)


if __name__ == "__main__":
    unittest.main()
