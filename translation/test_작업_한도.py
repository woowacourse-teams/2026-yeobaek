import sqlite3
import tempfile
import unittest

import 작업_한도 as quota


AX = """749 text 상위 채팅
751 table (TABLE)
752 data item (행)
753 data item (열 머리글) Chat
754 data item (열 머리글) 주간 한도의 %
755 button 주간 한도의 %
756 data item (열 머리글) 사용한 크레딧
757 button 사용한 크레딧
758 data item (행)
759 data item (행 헤더) Dedicated book fixture
760 button Dedicated book fixture
761 data item (항목) 35.32%
762 data item (항목) 0
800 text 플랜 사용 내역
810 text 주간 기간: 2026-11-12T23:59 UTC ~ 2026-11-19T23:59 UTC, 11월 17일 00:00 UTC 기준 사용량 정보가 최신 상태가 아닐 수 있습니다
"""


class QuotaTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.book = self.temp.name
        self.db = sqlite3.connect(":memory:")
        self.addCleanup(self.db.close)
        self.db.row_factory = sqlite3.Row
        quota.initialize(self.db)
        quota.bind_chat(self.db, self.book, "run", "thread", "Dedicated book fixture")

    def snapshot(self, raw=AX, time="2026-11-18T05:24:38.770Z"):
        return {"accessibilityText": raw, "observedAt": time, "source": quota.SOURCE, "appVersion": "fixture"}

    def test_observation_is_partial_and_deduplicated_without_raw_text(self):
        snapshot = self.snapshot()
        first = quota.import_snapshot(self.db, self.book, "run", "thread", snapshot)
        self.assertEqual(first, quota.import_snapshot(self.db, self.book, "run", "thread", snapshot))
        report = quota.quota_report(self.db, self.book)
        self.assertEqual((report["status"], report["coverage"], report["observationCount"]), ("observed_ui", "incomplete", 1))
        self.assertEqual(report["displayedWeeklyPercent"], 35.32)
        self.assertIsNone(report["percentPoints"])
        self.assertIsNone(report["dataAsOf"])
        self.assertIsNone(report["reportingRange"])
        self.assertTrue(report["warnings"])
        self.assertIn("11월 17일", report["windowContext"][0])
        self.assertNotIn("accessibilityText", self.db.execute("SELECT record FROM quota_observations").fetchone()[0])

    def test_immutable_and_duplicate_title(self):
        quota.bind_chat(self.db, self.book, "run", "thread", "Dedicated book fixture")
        for run, thread, title in [("new", "thread", "Dedicated book fixture"), ("run", "thread", "Renamed"), ("run", "other", "Dedicated book fixture")]:
            with self.assertRaises(ValueError):
                quota.bind_chat(self.db, self.book, run, thread, title)

    def test_fail_closed_for_bad_structure_or_values(self):
        row = AX[AX.index("758 data item"):AX.index("800 text")]
        for raw in [AX.replace("주간 한도의 %", "Account usage"), AX.replace("Dedicated book fixture", "Other chat"), AX.replace("800 text", row + "800 text"), "opaque", AX.replace("35.32%", "NaN%"), AX.replace("35.32%", "Infinity%"), AX.replace("35.32%", "-1%"), AX.replace("35.32%", "9" * 400 + "%"), AX.replace("상위 채팅", "\ufffd"), AX.encode("utf-8"), AX.replace("800 text 플랜 사용 내역", "")]:
            with self.subTest(raw=str(raw)[:50]), self.assertRaises(ValueError):
                quota.import_snapshot(self.db, self.book, "run", "thread", self.snapshot(raw))
        self.assertEqual(quota.quota_report(self.db, self.book)["status"], "unavailable")

    def test_untrusted_numeric_or_naive_timestamp_rejected(self):
        for update in [{"displayedWeeklyPercent": 1}, {"source": "manual"}, {"observedAt": "2026-11-18T05:00:00"}, {"appVersion": 12}]:
            with self.assertRaises(ValueError):
                quota.import_snapshot(self.db, self.book, "run", "thread", dict(self.snapshot(), **update))

    def test_latest_time_run_isolation_and_no_cross_chat_total(self):
        quota.import_snapshot(self.db, self.book, "run", "thread", self.snapshot(AX.replace("35.32%", "120.25%")))
        quota.import_snapshot(self.db, self.book, "run", "thread", self.snapshot(time="2026-11-17T00:00:00Z"))
        self.assertEqual(quota.quota_report(self.db, self.book, "run")["displayedWeeklyPercent"], 120.25)
        self.assertEqual(quota.quota_report(self.db, self.book, "other")["status"], "unavailable")
        quota.bind_chat(self.db, self.book, "other", "second", "Second fixture")
        quota.import_snapshot(self.db, self.book, "other", "second", self.snapshot(AX.replace("Dedicated book fixture", "Second fixture")))
        self.assertIsNone(quota.quota_report(self.db, self.book)["displayedWeeklyPercent"])
        self.assertEqual(len(quota.quota_report(self.db, self.book)["chatObservations"]), 2)

    def test_ledger_conflict_rejected(self):
        self.db.execute("CREATE TABLE bindings (session_id TEXT,book TEXT,run_id TEXT)")
        self.db.execute("INSERT INTO bindings VALUES ('ledger',?,'different')", (self.book,))
        self.db.commit()
        with self.assertRaises(ValueError):
            quota.bind_chat(self.db, self.book, "run", "ledger", "Ledger fixture")
        self.db.execute("INSERT INTO bindings VALUES ('thread',?,'different')", (self.book,))
        with self.assertRaises(ValueError):
            quota.import_snapshot(self.db, self.book, "run", "thread", self.snapshot())


if __name__ == "__main__":
    unittest.main()
