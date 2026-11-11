"""실제 보조 함수의 시간 제한, SHA 확인, worker 정상 종료 판정을 검증한다."""

import copy
import importlib.util
import json
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch


spec = importlib.util.spec_from_file_location("deploy_support", Path(__file__).resolve().parents[1] / "deploy_support.py")
support = importlib.util.module_from_spec(spec)
spec.loader.exec_module(support)
SHA = "a" * 40


class Clock:
    def __init__(self):
        self.now = 0

    def monotonic(self):
        return self.now

    def sleep(self, seconds):
        self.now += seconds


class HealthTest(unittest.TestCase):
    def setUp(self):
        self.clock = Clock()
        for name in ("monotonic", "sleep"):
            mock = patch.object(support.time, name, getattr(self.clock, name))
            mock.start()
            self.addCleanup(mock.stop)

    def response(self, sha, status="200", duration=0.1):
        def run(args, **kwargs):
            self.assertLessEqual(kwargs["timeout"], 3)
            self.clock.now += min(duration, kwargs["timeout"])
            self.assertEqual("-", args[args.index("--dump-header") + 1])
            output = "HTTP/1.1 " + status + " OK\r\nDeployed-Commit-SHA: " + sha + "\r\n\r\n\n" + status
            return subprocess.CompletedProcess(args, 0, output, "")
        return run

    def test_matching_sha_and_200_succeed(self):
        with patch.object(support.subprocess, "run", side_effect=self.response(SHA)) as run:
            support.health("https://example.test/health", SHA, 60)
        self.assertEqual(1, run.call_count)

    def test_wrong_sha_never_counts_as_ready_and_deadline_is_60_seconds(self):
        with patch.object(support.subprocess, "run", side_effect=self.response("b" * 40, duration=3)):
            with self.assertRaises(RuntimeError):
                support.health("https://example.test/health", SHA, 60)
        self.assertEqual(60, self.clock.now)

    def test_last_request_is_capped_to_remaining_budget(self):
        with patch.object(support.subprocess, "run", side_effect=self.response(SHA, "503", duration=3)) as run:
            with self.assertRaises(RuntimeError):
                support.health("https://example.test/health", SHA, 6)
        self.assertEqual(1, run.call_args.kwargs["timeout"])
        self.assertEqual(6, self.clock.now)

    def test_unknown_sha_is_not_a_rollback_baseline(self):
        with patch.object(support.subprocess, "run", side_effect=self.response("unknown")):
            with self.assertRaises(RuntimeError):
                support.health("https://example.test/health", "-", 1)

    def test_duplicate_sha_headers_are_rejected(self):
        with patch.object(support.subprocess, "run", side_effect=self.response(SHA + "\r\nDeployed-Commit-SHA: " + SHA)):
            with self.assertRaises(RuntimeError):
                support.health("https://example.test/health", SHA, 1)

    def test_health_results_are_grouped_and_failure_keeps_observed_sha(self):
        with tempfile.TemporaryDirectory() as directory:
            report = Path(directory) / "health-checks.json"
            with patch.object(support.subprocess, "run", side_effect=self.response(SHA)):
                support.health("https://example.test/health", SHA, 3, report, "active_app_via_domain")
            with patch.object(support.subprocess, "run", side_effect=self.response("b" * 40)):
                with self.assertRaises(RuntimeError):
                    support.health("http://localhost:8081/health", SHA, 1, report, "new_app_local")
            data = json.loads(report.read_text())
            self.assertEqual("passed", data["active_app_via_domain"]["status"])
            new_app_result = data["new_app_local"]
            self.assertIn("새 앱에 직접 요청", new_app_result["description"])
            self.assertEqual("failed", new_app_result["status"])
            self.assertEqual("b" * 40, new_app_result["observed_commit_sha"])
            self.assertTrue(new_app_result["checked_at"].endswith("+09:00"))


class MetadataTest(unittest.TestCase):
    def test_korean_descriptions_remain_when_final_result_is_added(self):
        with tempfile.TemporaryDirectory() as directory:
            report = Path(directory) / "deployment.json"
            support.metadata(report, "started", "example/app:" + SHA, SHA)
            support.metadata(report, "finished", "0", "complete", "1", "not-needed", "complete", "")
            data = json.loads(report.read_text())
            self.assertEqual("배포 시작 시각 (KST)", data["_descriptions"]["started_at"])
            self.assertIn("커밋 SHA", data["_descriptions"]["target"])
            self.assertEqual(SHA, data["target"]["commit_sha"])
            self.assertEqual(0, data["result"]["exit_code"])


class WorkerTest(unittest.TestCase):
    def setUp(self):
        self.before = {"master": {"pid": "10", "started": "100", "parent": "1"},
                       "workers": [{"pid": "11", "started": "110", "parent": "10"}]}
        self.current = copy.deepcopy(self.before)
        self.current["workers"] = [{"pid": "12", "started": "120", "parent": "10"}]
        self.events = "[notice] 11#11: gracefully shutting down\n[notice] 10#10: worker process 11 exited with code 0\n"

    def test_normal_exit_requires_both_disappearance_and_logs(self):
        self.assertTrue(support.worker_result(self.before, self.current, self.events))
        self.assertFalse(support.worker_result(self.before, self.before, self.events))
        self.assertFalse(support.worker_result(self.before, self.current, ""))

    def test_signal_exit_is_not_normal_completion(self):
        with self.assertRaises(RuntimeError):
            support.worker_result(self.before, self.current, "worker process 11 exited on signal 9")

    def test_master_restart_is_not_successful_drain(self):
        self.current["master"]["started"] = "999"
        with self.assertRaises(RuntimeError):
            support.worker_result(self.before, self.current, self.events)

    def test_pid_reuse_is_rejected(self):
        self.current["workers"][0]["pid"] = "11"
        with self.assertRaises(RuntimeError):
            support.worker_result(self.before, self.current, self.events)

    def test_force_close_setting_is_rejected_but_comment_is_ignored(self):
        with tempfile.TemporaryDirectory() as directory:
            config = Path(directory) / "nginx.conf"
            config.write_text("# worker_shutdown_timeout 30s;\nworker_shutdown_timeout 0;\n")
            support.check_timeout(config)
            config.write_text("worker_shutdown_timeout 30s;\n")
            with self.assertRaises(RuntimeError):
                support.check_timeout(config)

    def test_rollback_snapshot_includes_first_reload_log_position(self):
        with tempfile.TemporaryDirectory() as directory:
            previous = Path(directory) / "old.json"
            target = Path(directory) / "rollback.json"
            position = {"device": "1", "inode": "2", "size": 100}
            previous.write_text(json.dumps({"position": position}))
            state = {**self.before, "draining": ["11"]}
            with patch.object(support, "processes", return_value=state), \
                    patch.object(support, "log_position", return_value={**position, "size": 200}):
                support.snapshot(target, "/example/error.log", previous)
            self.assertEqual(position, json.loads(target.read_text())["position"])

    def test_rotated_log_does_not_confirm_worker_completion(self):
        with tempfile.TemporaryDirectory() as directory:
            snapshot = Path(directory) / "workers.json"
            snapshot.write_text(json.dumps({**self.before, "log": "/example/error.log",
                                           "position": {"device": "1", "inode": "2", "size": 100}}))
            with patch.object(support, "log_position", return_value={"device": "1", "inode": "3", "size": 200}):
                with self.assertRaises(RuntimeError):
                    support.wait_workers(str(snapshot), 120, str(snapshot.with_suffix(".log")))


class RetentionTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)

    def records(self, count):
        paths = [self.root / f"{SHA}__202601{day:02d}-000000-KST__aaaaaa" for day in range(1, count + 1)]
        for path in paths:
            path.mkdir()
            (path / "deployment.log").write_text("record")
        return paths

    def test_keeps_current_and_latest_nineteen_other_records(self):
        paths = self.records(25)
        support.prune_records(paths[-1])
        self.assertEqual(set(paths[5:]), set(self.root.iterdir()))

    def test_current_record_is_protected_even_if_its_timestamp_is_older(self):
        paths = self.records(25)
        support.prune_records(paths[0])
        self.assertEqual({paths[0], *paths[6:]}, set(self.root.iterdir()))

    def test_retention_sorts_by_time_not_commit_sha(self):
        paths = self.records(21)
        oldest = paths[0].with_name(paths[0].name.replace(SHA, "f" * 40))
        paths[0].rename(oldest)
        newest = paths[-1].with_name(paths[-1].name.replace(SHA, "0" * 40))
        paths[-1].rename(newest)
        support.prune_records(newest)
        self.assertFalse(oldest.exists())
        self.assertTrue(newest.exists())

    def test_fewer_than_twenty_records_are_unchanged(self):
        paths = self.records(3)
        support.prune_records(paths[-1])
        self.assertEqual(set(paths), set(self.root.iterdir()))

    def test_unrelated_directories_and_symlink_targets_are_untouched(self):
        paths = self.records(25)
        outside = self.root / "manual-backup"
        outside.mkdir()
        (outside / "keep.txt").write_text("keep")
        link = self.root / f"{SHA}__20200101-000000-KST__aaaaaa"
        link.symlink_to(outside, target_is_directory=True)
        support.prune_records(paths[-1])
        self.assertTrue(link.is_symlink())
        self.assertEqual("keep", (outside / "keep.txt").read_text())
        self.assertEqual(22, len(list(self.root.iterdir())))

    def test_deletion_failure_is_reported_and_other_old_records_are_processed(self):
        paths = self.records(22)
        actual_remove = support.shutil.rmtree

        def remove(path):
            if path == paths[1]:
                raise PermissionError("test denied")
            actual_remove(path)

        with patch.object(support.shutil, "rmtree", side_effect=remove):
            with self.assertRaises(RuntimeError):
                support.prune_records(paths[-1])
        self.assertFalse(paths[0].exists())
        self.assertTrue(paths[1].exists())
        self.assertTrue(paths[-1].exists())

    def test_missing_current_record_prevents_any_deletion(self):
        paths = self.records(25)
        with self.assertRaises(RuntimeError):
            support.prune_records(self.root / f"{SHA}__20260201-000000-KST__aaaaaa")
        self.assertTrue(all(path.exists() for path in paths))


class TemporaryCleanupTest(unittest.TestCase):
    def test_cleanup_does_not_follow_nested_symlink(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            record = root / "records" / f"{SHA}__20260101-000000-KST__aaaaaa"
            record.mkdir(parents=True)
            outside = root / "outside"
            outside.mkdir()
            protected = outside / ".workers-before-switch.json.tmp"
            protected.write_text("keep")
            (record / "nginx").symlink_to(outside, target_is_directory=True)
            own_temp = record / ".health-checks.json.tmp"
            own_temp.write_text("remove")
            with patch.object(support.subprocess, "run"):
                support.cleanup_temporary(root, "/example/nginx.conf")
            self.assertEqual("keep", protected.read_text())
            self.assertFalse(own_temp.exists())


if __name__ == "__main__":
    unittest.main()
