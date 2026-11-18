"""Conservative import of Codex Desktop Korean accessibility observations.

The AI opens Settings > Usage > Analytics with CUA and passes its accessibility
text here. No screenshot, account percentage, or token ratio is a measurement.
"""
from datetime import datetime
from decimal import Decimal
import hashlib
import json
import math
from pathlib import Path
import re

SOURCE = "codex_desktop_accessibility"
REASON = "Chat display observations do not establish full-book, child inclusion, reporting period, or freshness coverage."
NODE = re.compile(r"^\s*(?:-\s*)?\d+\s+(text|table|data item|button|column header|row|cell)(?:\s+\(([^)]*)\))?(?:\s+(.*))?$")


def initialize(db):
    db.executescript("""
      CREATE TABLE IF NOT EXISTS quota_chats (
        thread_id TEXT PRIMARY KEY, book TEXT NOT NULL, run_id TEXT NOT NULL,
        chat_title TEXT NOT NULL UNIQUE);
      CREATE TABLE IF NOT EXISTS quota_observations (
        observation_key TEXT PRIMARY KEY, thread_id TEXT NOT NULL,
        observed_epoch REAL NOT NULL, record TEXT NOT NULL);
    """)


def bind_chat(db, book, run_id, thread_id, chat_title):
    book = str(Path(book).resolve())
    if not Path(book).is_dir() or any(not isinstance(v, str) or not v.strip()
                                     for v in (run_id, thread_id, chat_title)):
        raise ValueError("Existing book, run, thread and unique exact chat title required")
    with db:
        # Serialize with the token ledger's immutable assignment check too.
        db.execute("BEGIN IMMEDIATE")
        old = db.execute("SELECT book,run_id,thread_id,chat_title FROM quota_chats WHERE thread_id=?", (thread_id,)).fetchone()
        values = (book, run_id, thread_id, chat_title)
        if old:
            if tuple(old) != values:
                raise ValueError("Chat book/run/title binding is immutable")
        else:
            if db.execute("SELECT 1 FROM quota_chats WHERE chat_title=?", (chat_title,)).fetchone():
                raise ValueError("Duplicate chat title is ambiguous")
            # Enforce attribution agreement when the token ledger already knows this thread.
            if db.execute("SELECT 1 FROM sqlite_master WHERE name='bindings'").fetchone():
                ledger = db.execute("SELECT book,run_id FROM bindings WHERE session_id=?", (thread_id,)).fetchone()
                if ledger and tuple(ledger) != (book, run_id):
                    raise ValueError("Chat conflicts with session ledger attribution")
            db.execute("INSERT INTO quota_chats VALUES (?,?,?,?)", (thread_id, book, run_id, chat_title))
    return dict(zip(("book", "run_id", "thread_id", "chat_title"), values))


def _parse(raw, title):
    if not isinstance(raw, str) or "\ufffd" in raw:
        raise ValueError("Valid UTF-8 accessibility text required")
    raw.encode("utf-8", errors="strict")
    nodes = [m.groups() for line in raw.splitlines() if (m := NODE.fullmatch(line))]
    sections = [i for i, n in enumerate(nodes) if n[2] == "상위 채팅"]
    if len(sections) != 1:
        raise ValueError("One Korean top-chat section required")
    start = sections[0] + 1
    ends = [i for i in range(start, len(nodes)) if nodes[i][2] == "플랜 사용 내역"]
    if not ends:
        raise ValueError("Top-chat section boundary missing")
    table = nodes[start:ends[0]]
    if sum(n[0] == "table" for n in table) != 1:
        raise ValueError("One top-chat table required")
    table = table[next(i for i, n in enumerate(table) if n[0] == "table") + 1:]
    # Desktop exposes its header as a row too, with sortable button children.
    header_cells = [i for i, n in enumerate(table) if n[:2] == ("data item", "열 머리글")]
    if header_cells:
        headers = [table[i][2] for i in header_cells]
        first_row = next((i for i in range(header_cells[-1] + 1, len(table))
                          if table[i][:2] == ("data item", "행")), len(table))
    else:
        first_row = next((i for i, n in enumerate(table) if n[:2] == ("data item", "행")), len(table))
        headers = [n[2] for n in table[:first_row] if n[2]]
    if headers != ["Chat", "주간 한도의 %", "사용한 크레딧"]:
        raise ValueError("Exact Korean top-chat column headers required")
    rows, current = [], None
    for node in table[first_row:]:
        if node[:2] == ("data item", "행"):
            current = []
            rows.append(current)
        elif current is not None:
            current.append(node)
    matching = [row for row in rows if any(n[:2] == ("data item", "행 헤더") and n[2] == title for n in row)]
    if len(matching) != 1:
        raise ValueError("Bound chat must match exactly one table row")
    row = matching[0]
    if [n[2] for n in row if n[:2] == ("data item", "행 헤더")] != [title]:
        raise ValueError("Ambiguous chat row header")
    cells = [n[2] for n in row if n[:2] == ("data item", "항목")]
    if len(cells) != 2 or not re.fullmatch(r"\d+(?:\.\d+)?%", cells[0] or ""):
        raise ValueError("Exact displayed percentage and credit cells required")
    percent = float(Decimal(cells[0][:-1]))
    if not math.isfinite(percent) or not re.fullmatch(r"\d+(?:\.\d+)?", cells[1] or ""):
        raise ValueError("Finite nonnegative displayed values required")
    windows = [n[2] for n in nodes if n[0] == "text" and (n[2] or "").startswith("주간 기간:")]
    return {"displayedWeeklyPercent": percent, "displayedWeeklyPercentText": cells[0],
            "displayedCreditsText": cells[1], "reportingRange": None,
            "windowContext": windows, "dataAsOf": None,
            "warnings": ["사용량 정보가 최신 상태가 아닐 수 있습니다"] if "사용량 정보가 최신 상태가 아닐 수 있습니다" in raw else []}


def import_snapshot(db, book, run_id, thread_id, snapshot):
    book = str(Path(book).resolve())
    binding = db.execute("SELECT chat_title FROM quota_chats WHERE book=? AND run_id=? AND thread_id=?", (book, run_id, thread_id)).fetchone()
    if not binding:
        raise ValueError("Bind a dedicated chat before importing observations")
    if db.execute("SELECT 1 FROM sqlite_master WHERE name='bindings'").fetchone():
        ledger = db.execute("SELECT book,run_id FROM bindings WHERE session_id=?", (thread_id,)).fetchone()
        if ledger and tuple(ledger) != (book, run_id):
            raise ValueError("Chat conflicts with session ledger attribution")
    if (not isinstance(snapshot, dict) or set(snapshot) - {"accessibilityText", "observedAt", "appVersion", "source"}
            or snapshot.get("source") != SOURCE):
        raise ValueError("Only accessibility-source snapshots are accepted; numeric measurements are forbidden")
    observed = snapshot.get("observedAt")
    if not isinstance(observed, str):
        raise ValueError("Timezone-aware observation time required")
    moment = datetime.fromisoformat(observed.replace("Z", "+00:00"))
    if moment.tzinfo is None or moment.utcoffset() is None:
        raise ValueError("Timezone-aware observation time required")
    if snapshot.get("appVersion") is not None and not isinstance(snapshot["appVersion"], str):
        raise ValueError("App version must be text")
    raw = snapshot.get("accessibilityText")
    record = _parse(raw, binding[0])
    digest = hashlib.sha256(raw.encode("utf-8")).hexdigest()
    identity = {"book": book, "run_id": run_id, "thread_id": thread_id,
                "chatTitle": binding[0], "source": SOURCE, "observedAt": observed,
                "appVersion": snapshot.get("appVersion"), "accessibilitySha256": digest}
    key = hashlib.sha256(json.dumps(identity, sort_keys=True, ensure_ascii=False).encode("utf-8")).hexdigest()
    record.update(identity, observationKey=key)
    with db:
        db.execute("INSERT OR IGNORE INTO quota_observations VALUES (?,?,?,?)",
                   (key, thread_id, moment.timestamp(), json.dumps(record, ensure_ascii=False)))
    return record


def quota_report(db, book, run_id=None):
    book = str(Path(book).resolve())
    rows = db.execute("""SELECT o.record FROM quota_observations o JOIN quota_chats c USING(thread_id)
                         WHERE c.book=? AND (? IS NULL OR c.run_id=?)
                         ORDER BY o.observed_epoch DESC, o.observation_key DESC""", (book, run_id, run_id)).fetchall()
    latest = {}
    for row in rows:
        record = json.loads(row[0])
        latest.setdefault(record["thread_id"], record)
    result = {"status": "observed_ui" if latest else "unavailable", "percentPoints": None,
              "coverage": "incomplete", "reason": REASON if latest else "No validated bound-chat UI observations available.",
              "displayedWeeklyPercent": None, "chatObservations": list(latest.values()),
              "observationCount": len(rows)}
    if len(latest) == 1:
        record = next(iter(latest.values()))
        for field in ("displayedWeeklyPercent", "displayedWeeklyPercentText", "source", "observedAt", "reportingRange", "windowContext", "dataAsOf", "warnings"):
            result[field] = record[field]
    return result
