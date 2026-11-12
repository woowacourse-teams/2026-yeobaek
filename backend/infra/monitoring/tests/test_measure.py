import io
import json
import os
from pathlib import Path
import sys
import unittest
from unittest.mock import patch
import urllib.error

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import measure


def job(step_conclusion, status="in_progress"):
    return {measure.MONITOR_JOB: {"status": status, "steps": [{
        "name": measure.READY_STEP,
        "conclusion": step_conclusion,
        "completed_at": measure.timestamp(),
    }]}}


class WaitForMonitorTest(unittest.TestCase):
    @patch.object(measure.time, "sleep")
    @patch.object(measure, "jobs")
    def test_waits_for_first_probe(self, jobs, sleep):
        jobs.side_effect = [job(None), job("success")]
        measure.wait_for_monitor()
        self.assertEqual(jobs.call_count, 2)

    @patch.object(measure, "jobs")
    def test_completed_monitor_cannot_release_deploy(self, jobs):
        jobs.return_value = job("success", status="completed")
        with self.assertRaisesRegex(RuntimeError, "did not start"):
            measure.wait_for_monitor()

    @patch.object(measure.urllib.request, "urlopen")
    def test_only_reads_jobs_from_current_attempt(self, urlopen):
        env = {"GITHUB_API_URL": "https://api.github.com", "GITHUB_REPOSITORY": "team/repo",
               "GITHUB_RUN_ID": "123", "GITHUB_RUN_ATTEMPT": "2", "GH_TOKEN": "token"}
        urlopen.return_value.__enter__.return_value = io.StringIO('{"jobs":[]}')
        with patch.dict(os.environ, env):
            self.assertEqual(measure.jobs(), {})
        self.assertIn("/actions/runs/123/attempts/2/jobs", urlopen.call_args.args[0].full_url)


class BetterStackTest(unittest.TestCase):
    def test_payload_contains_failures_and_their_recovery(self):
        samples = [
            {"sequence": i, "startedAt": f"2026-11-12T00:00:0{i}+00:00",
             "success": status == 200, "statusCode": status, "commitSha": None}
            for i, status in enumerate([200, 502, 0, 200, 502])
        ]
        summary = measure.summarize(samples)
        summary["stoppedAt"] = samples[-1]["startedAt"]
        context = {"environment": "dev", "runId": 123, "runAttempt": 2}
        with patch.object(measure.urllib.request, "build_opener") as factory:
            factory.return_value.open.return_value.__enter__.return_value.status = 202
            measure.send_logs("https://ingest.example", "token", context, samples, summary)
        events = json.loads(factory.return_value.open.call_args.args[0].data)
        self.assertEqual([event["statusCode"] for event in events[:-1]], [200, 502, 0, 200, 502])
        self.assertEqual(events[1]["dt"], samples[1]["startedAt"])
        result = events[-1]
        self.assertEqual((result["environment"], result["runId"], result["runAttempt"]), ("dev", 123, 2))
        self.assertEqual((result["failures"], result["estimatedRecoveredSeconds"]), (3, 2))
        self.assertTrue(result["recoveryUnconfirmed"])

    @patch.object(measure.urllib.request, "build_opener")
    def test_rejected_ingestion_is_reported_as_failure(self, factory):
        factory.return_value.open.side_effect = urllib.error.HTTPError("", 403, "", {}, None)
        with self.assertRaisesRegex(RuntimeError, "Better Stack returned HTTP 403"):
            measure.send_logs("https://ingest.example", "token", {}, [], {"stoppedAt": measure.timestamp()})
