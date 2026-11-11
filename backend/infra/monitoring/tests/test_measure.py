import json
import os
from pathlib import Path
import sys
import tempfile
import threading
import time
import unittest
import subprocess
from unittest.mock import Mock, patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import measure

SESSION = "11111111-1111-1111-1111-111111111111"
ENV = {"MONITOR_ENVIRONMENT": "dev", "MONITOR_SESSION": SESSION,
       "MONITOR_DEV_HEALTH_URL": "https://example.com/health", "GITHUB_RUN_ID": "20", "GITHUB_RUN_ATTEMPT": "1"}


def sample(sequence, success, sha="a" * 40):
    return {"sequence": sequence, "startedAt": f"2026-11-11T00:00:{sequence:02d}+00:00",
            "success": success, "commitSha": sha, "statusCode": 200 if success else 502,
            "elapsedMs": 20, "error": None if success else "http_error"}


def deployment(run_id, status="completed", end="2026-11-11T00:02:00Z"):
    return {"id": run_id, "run_attempt": 1, "status": status, "head_branch": "develop",
            "path": ".github/workflows/backend-dev.yml", "updated_at": end}


class SummaryTest(unittest.TestCase):
    def test_counts_outages_by_request_start_not_completion_order_or_timeout(self):
        result = measure.summarize([sample(4, True, "b" * 40), sample(2, False), sample(0, True),
                                    sample(1, False), sample(3, True, "b" * 40)])
        self.assertEqual((result["total"], result["successes"], result["failures"]), (5, 3, 2))
        self.assertEqual(result["outages"][0]["estimatedSeconds"], 2)
        self.assertEqual(result["estimatedRecoveredSeconds"], 2)
        self.assertEqual([item["commitSha"] for item in result["versions"]], ["a" * 40, "b" * 40])

    def test_unrecovered_failure_has_no_invented_duration(self):
        result = measure.summarize([sample(0, False), sample(1, True), sample(2, False)])
        self.assertTrue(result["recoveryUnconfirmed"])
        self.assertIsNone(result["outages"][-1]["recoveredAt"])
        self.assertIsNone(result["outages"][-1]["estimatedSeconds"])
        self.assertEqual(result["estimatedRecoveredSeconds"], 1)

    def test_zero_failures_and_missing_version(self):
        result = measure.summarize([sample(0, True, None)])
        self.assertEqual(result["failures"], 0)
        self.assertEqual(result["outages"], [])
        self.assertEqual(result["versions"], [])

    def test_empty_measurement_does_not_claim_an_outage(self):
        self.assertEqual(measure.summarize([])["total"], 0)


class ProbeTest(unittest.TestCase):
    def test_success_requires_200_and_records_sha_without_sending_credentials(self):
        process = Mock()
        process.communicate.return_value = ("200\n0.02\n" + "a" * 40, "")
        process.returncode = 0
        with patch.object(measure.subprocess, "Popen") as popen:
            popen.return_value.__enter__.return_value = process
            response = measure.probe("https://example.com/health", SESSION, 0)
        self.assertTrue(response["success"])
        self.assertEqual(response["commitSha"], "a" * 40)
        command = popen.call_args.args[0]
        self.assertEqual(command[command.index("--max-time") + 1], "3")
        self.assertNotIn("--location", command)
        self.assertFalse(any("Authorization" in argument for argument in command))

    def test_timed_out_body_is_failure_even_after_200_headers(self):
        process = Mock()
        process.communicate.return_value = ("200\n3.0\nunknown", "timeout")
        process.returncode = 28
        with patch.object(measure.subprocess, "Popen") as popen:
            popen.return_value.__enter__.return_value = process
            response = measure.probe("https://example.com/health", SESSION, 0)
        self.assertFalse(response["success"])
        self.assertEqual(response["error"], "timeout")

    def test_hung_curl_is_killed_and_recorded_as_timeout(self):
        process = Mock()
        process.communicate.side_effect = [subprocess.TimeoutExpired("curl", 4), ("", "")]
        with patch.object(measure.subprocess, "Popen") as popen:
            popen.return_value.__enter__.return_value = process
            response = measure.probe("https://example.com/health", SESSION, 0)
        process.kill.assert_called_once()
        self.assertEqual(response["error"], "timeout")


class ControlsTest(unittest.TestCase):
    def setUp(self):
        self.github = Mock()
        self.github.artifacts.return_value = []
        self.github.pages.return_value = []
        self.controls = measure.Controls(self.github, "dev", SESSION)

    def test_completed_deployment_uses_job_finish_time(self):
        self.controls.links[(1, 1)] = deployment(1)
        self.github.request.return_value = deployment(1)
        self.github.pages.return_value = [{"started_at": "2026-11-11T00:00:00Z",
                                           "completed_at": "2026-11-11T00:01:00Z"}]
        self.controls.refresh()
        self.assertEqual(self.controls.deadline, measure.epoch("2026-11-11T00:06:00Z"))

    def test_ongoing_rollback_clears_previous_countdown(self):
        self.controls.links = {(1, 1): deployment(1), (2, 1): deployment(2, "in_progress")}
        self.github.request.side_effect = [deployment(1), deployment(2, "in_progress")]
        self.controls.deadline = 100
        self.controls.refresh()
        self.assertIsNone(self.controls.deadline)

    def test_rollbacks_finish_later_and_get_five_minutes(self):
        self.controls.links = {(1, 1): deployment(1), (2, 1): deployment(2)}
        self.github.request.side_effect = [deployment(1), deployment(2, end="2026-11-11T00:04:00Z")]
        self.controls.refresh()
        self.assertEqual(self.controls.deadline, measure.epoch("2026-11-11T00:09:00Z"))

    def test_api_failure_never_implies_deployment_completed(self):
        self.controls.deadline = 100
        self.github.artifacts.side_effect = OSError("network")
        self.controls.refresh()
        self.assertIsNone(self.controls.deadline)
        self.assertEqual(self.controls.error_count, 1)

    def test_stop_command_is_idempotent(self):
        artifact = {"id": 1, "name": f"ym-command-dev-{SESSION}-7", "expired": False,
                    "workflow_run": {"id": 7}}
        self.github.artifacts.return_value = [artifact]
        self.github.request.return_value = {"path": measure.WORKFLOW, "display_title": f"monitor:dev:{SESSION}:stop"}
        self.github.artifact_json.return_value = {"command": "stop"}
        self.controls.refresh()
        self.controls.refresh()
        self.assertTrue(self.controls.stop)
        self.github.artifact_json.assert_called_once_with(1, "command.json")

    def test_command_from_another_workflow_is_not_executed(self):
        self.github.artifacts.return_value = [{"id": 1, "name": f"ym-command-dev-{SESSION}-7",
                                               "expired": False, "workflow_run": {"id": 7}}]
        self.github.request.return_value = {"path": ".github/workflows/unrelated.yml"}
        self.controls.refresh()
        self.assertFalse(self.controls.stop)
        self.github.artifact_json.assert_not_called()


class RunnerTest(unittest.TestCase):
    @patch.dict(os.environ, ENV, clear=True)
    def test_dev_without_target_never_falls_back_to_prod(self):
        del os.environ["MONITOR_DEV_HEALTH_URL"]
        os.environ["MONITOR_PROD_HEALTH_URL"] = "https://production.example/health"
        with self.assertRaises(ValueError):
            measure.settings()

    @patch.dict(os.environ, ENV, clear=True)
    def test_queued_duplicate_is_rejected_after_previous_measurement_finishes(self):
        github = Mock()
        github.request.return_value = {"created_at": "2026-11-11T00:01:00Z"}
        github.pages.return_value = [{"id": 10, "display_title": f"monitor:dev:{SESSION}:start",
                                     "status": "completed", "updated_at": "2026-11-11T00:10:00Z"}]
        with patch.object(measure, "GitHub", return_value=github), self.assertRaises(ValueError):
            measure.guard()

    @patch.dict(os.environ, ENV, clear=True)
    def test_rerun_is_rejected_to_keep_results_immutable(self):
        os.environ["GITHUB_RUN_ATTEMPT"] = "2"
        with patch.object(measure, "GitHub"), self.assertRaises(ValueError):
            measure.guard()

    @patch.dict(os.environ, ENV, clear=True)
    def test_cleanup_keeps_twenty_and_does_not_touch_other_artifacts(self):
        artifacts = []
        for index in range(21):
            session = SESSION if index == 20 else f"{index:08d}-1111-1111-1111-111111111111"
            artifacts.append({"id": index + 1, "name": f"ym-result-dev-{session}", "expired": False,
                              "created_at": f"2026-11-11T00:00:{index:02d}Z"})
        artifacts += [{"id": 98, "name": "android-ci-reports", "expired": False},
                      {"id": 99, "name": f"ym-result-prod-{SESSION}", "expired": False}]
        github = Mock()
        github.artifacts.return_value = artifacts
        with patch.object(measure, "GitHub", return_value=github):
            measure.cleanup()
        github.request.assert_called_once_with("/actions/artifacts/1", "DELETE")

    @patch.dict(os.environ, ENV, clear=True)
    def test_cleanup_requires_new_result_first(self):
        github = Mock()
        github.artifacts.return_value = []
        with patch.object(measure, "GitHub", return_value=github), self.assertRaises(ValueError):
            measure.cleanup()
        github.request.assert_not_called()

    @patch.dict(os.environ, ENV, clear=True)
    def test_slow_requests_overlap_and_results_are_saved_after_draining(self):
        running, peak = 0, 0
        lock = threading.Lock()

        def slow_probe(url, session, sequence, ready=None):
            nonlocal running, peak
            with lock:
                running += 1
                peak = max(peak, running)
            if ready:
                ready.set()
            time.sleep(0.15)
            with lock:
                running -= 1
            return sample(sequence, True)

        with tempfile.TemporaryDirectory() as directory:
            os.environ["MONITOR_OUTPUT"] = directory
            with patch.object(measure, "GitHub"), patch.object(measure.Controls, "poll"), \
                    patch.object(measure, "MAX_DURATION", 0.25), patch.object(measure, "INTERVAL", 0.05), \
                    patch.object(measure, "probe", side_effect=slow_probe):
                self.assertEqual(measure.measure(), 0)
            result = json.loads((Path(directory) / "result.json").read_text())
            self.assertGreater(peak, 1)
            self.assertEqual(running, 0)
            self.assertGreaterEqual(result["summary"]["total"], 3)
            self.assertEqual(result["stopReason"], "maximum_duration")

    @patch.dict(os.environ, ENV, clear=True)
    def test_manual_stop_saves_partial_result(self):
        def immediate(url, session, sequence, ready=None):
            if ready:
                ready.set()
            return sample(sequence, True)

        def stop(controls, finished):
            controls.stop = True

        with tempfile.TemporaryDirectory() as directory:
            os.environ["MONITOR_OUTPUT"] = directory
            with patch.object(measure, "GitHub"), patch.object(measure.Controls, "poll", stop), \
                    patch.object(measure, "probe", side_effect=immediate):
                measure.measure()
            result = json.loads((Path(directory) / "result.json").read_text())
            self.assertEqual(result["stopReason"], "manual")
            self.assertEqual(result["summary"]["total"], 1)


if __name__ == "__main__":
    unittest.main()
