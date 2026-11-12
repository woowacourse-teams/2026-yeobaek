"""Measure public HTTPS availability around a deploy and send logs to Better Stack."""
import concurrent.futures
import datetime as dt
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import threading
import time
import urllib.error
import urllib.parse
import urllib.request

INTERVAL = 1
REQUEST_TIMEOUT = 3
PRE_DEPLOY_CHECKS = 3
MAX_DURATION = 1800
AFTER_DEPLOYMENT = 300
DEPLOYMENT_POLL_INTERVAL = 10
READY_STEP = "Record three pre-deploy health checks"
MONITOR_JOB = "Measure deployment availability"
DEPLOY_JOB = "Deploy application"


def timestamp():
    return dt.datetime.now(dt.timezone.utc).isoformat(timespec="milliseconds")


def epoch(value):
    return dt.datetime.fromisoformat(value.replace("Z", "+00:00")).timestamp()


def probe(url, session, sequence):
    started = timestamp()
    begin = time.monotonic()
    separator = "&" if "?" in url else "?"
    target = f"{url}{separator}monitor_probe={session}-{sequence}"
    try:
        with subprocess.Popen([
            "curl", "--silent", "--show-error", "--proto", "=https",
            "--max-time", str(REQUEST_TIMEOUT), "--output", os.devnull,
            "--header", "Cache-Control: no-cache, no-store",
            "--write-out", "%{http_code}\n%{time_total}\n%header{deployed-commit-sha}", target,
        ], stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True) as process:
            try:
                stdout, _ = process.communicate(timeout=REQUEST_TIMEOUT + 1)
            except subprocess.TimeoutExpired:
                process.kill()
                process.communicate()
                raise
            returncode = process.returncode
        parts = stdout.splitlines()
        status = int(parts[0]) if parts and parts[0].isdigit() else 0
        sha = parts[2].strip() if len(parts) > 2 else ""
        if not re.fullmatch(r"[a-fA-F0-9]{7,64}|unknown", sha):
            sha = ""
        error = "timeout" if returncode == 28 else (
            "connection_error" if returncode else ("" if status == 200 else "http_error"))
    except subprocess.TimeoutExpired:
        status, sha, error = 0, "", "timeout"
    elapsed = round((time.monotonic() - begin) * 1000, 2)
    if elapsed > REQUEST_TIMEOUT * 1000 and not error:
        error = "timeout"
    return {"sequence": sequence, "startedAt": started, "statusCode": status,
            "elapsedMs": elapsed, "commitSha": sha or None,
            "success": status == 200 and not error, "error": error or None}


def summarize(samples):
    ordered = sorted(samples, key=lambda sample: sample["sequence"])
    outages, versions, start = [], [], None
    for sample in ordered:
        if not sample["success"] and start is None:
            start = sample["startedAt"]
        if sample["success"] and start is not None:
            outages.append({"startedAt": start, "recoveredAt": sample["startedAt"],
                            "estimatedSeconds": round(epoch(sample["startedAt"]) - epoch(start), 3)})
            start = None
        sha = sample["commitSha"]
        if sample["success"] and sha and (not versions or versions[-1]["commitSha"] != sha):
            versions.append({"at": sample["startedAt"], "commitSha": sha})
    if start is not None:
        outages.append({"startedAt": start, "recoveredAt": None, "estimatedSeconds": None})
    failures = sum(not sample["success"] for sample in ordered)
    pre_deploy = [sample for sample in ordered if sample.get("phase") == "pre_deploy"]
    return {"total": len(ordered), "successes": len(ordered) - failures, "failures": failures,
            "preDeployChecks": len(pre_deploy),
            "preDeployFailures": sum(not sample["success"] for sample in pre_deploy),
            "outages": outages, "versions": versions,
            "estimatedRecoveredSeconds": round(sum(item["estimatedSeconds"] or 0 for item in outages), 3),
            "recoveryUnconfirmed": start is not None}


def jobs():
    api = os.environ["GITHUB_API_URL"]
    repo = os.environ["GITHUB_REPOSITORY"]
    run = os.environ["GITHUB_RUN_ID"]
    attempt = os.environ["GITHUB_RUN_ATTEMPT"]
    request = urllib.request.Request(
        f"{api}/repos/{repo}/actions/runs/{run}/attempts/{attempt}/jobs?per_page=100",
        headers={"Authorization": f"Bearer {os.environ['GH_TOKEN']}",
                 "Accept": "application/vnd.github+json"},
    )
    with urllib.request.urlopen(request, timeout=5) as response:
        return {job["name"]: job for job in json.load(response)["jobs"]}


def wait_for_monitor():
    deadline = time.monotonic() + 120
    while time.monotonic() < deadline:
        try:
            monitor = jobs().get(MONITOR_JOB, {})
        except OSError:
            time.sleep(3)
            continue
        if monitor.get("status") == "completed":
            break
        for step in monitor.get("steps", []):
            if step["name"] == READY_STEP and step.get("conclusion") == "success":
                # Reserve time for the deploy to start even if the EC2 runner queued.
                if time.time() - epoch(step["completed_at"]) < MAX_DURATION - AFTER_DEPLOYMENT:
                    return
                raise RuntimeError("External measurement is too close to its time limit")
        time.sleep(3)
    raise RuntimeError("Pre-deploy health checks did not complete for this workflow attempt")


class DeploymentStatus:
    def __init__(self):
        self.completed = None
        self.conclusion = None

    def poll(self, finished):
        while not finished.is_set():
            try:
                deploy = jobs().get(DEPLOY_JOB, {})
                if deploy.get("status") == "completed":
                    self.conclusion = deploy["conclusion"]
                    self.completed = deploy["completed_at"]
                    return
            except OSError:
                # An unavailable API is not evidence that deploy finished.
                pass
            finished.wait(DEPLOYMENT_POLL_INTERVAL)


def measure(url, session, ready_path, deployment):
    started, begin = timestamp(), time.monotonic()
    pending, samples = [], []
    finished = threading.Event()
    poller = threading.Thread(target=deployment.poll, args=(finished,), daemon=True)
    next_sequence, skipped, reason = 0, 0, "maximum_duration"
    pre_deploy_completed = 0
    try:
        with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
            while time.monotonic() - begin < MAX_DURATION:
                if deployment.completed and time.time() >= epoch(deployment.completed) + AFTER_DEPLOYMENT:
                    reason = "deployment_finished"
                    break
                elapsed = time.monotonic() - begin
                if elapsed >= next_sequence * INTERVAL:
                    sequence = next_sequence
                    if sequence >= PRE_DEPLOY_CHECKS:
                        sequence = int(elapsed // INTERVAL)
                    skipped += sequence - next_sequence
                    pending.append(pool.submit(probe, url, session, sequence))
                    next_sequence = sequence + 1
                completed = [future for future in pending if future.done()]
                for future in completed:
                    sample = future.result()
                    if sample["sequence"] < PRE_DEPLOY_CHECKS:
                        sample["phase"] = "pre_deploy"
                        pre_deploy_completed += 1
                    samples.append(sample)
                    pending.remove(future)
                    if sample.get("phase") == "pre_deploy" and pre_deploy_completed == PRE_DEPLOY_CHECKS:
                        ready_path.touch()
                        poller.start()
                finished.wait(0.05)
            stopped = timestamp()
            samples.extend(future.result() for future in pending)
    finally:
        finished.set()
    samples.sort(key=lambda sample: sample["sequence"])
    summary = summarize(samples)
    summary.update(startedAt=started, stoppedAt=stopped, stopReason=reason,
                   skipped=skipped, deploymentConclusion=deployment.conclusion,
                   deploymentCompletedAt=deployment.completed)
    return samples, summary


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def send_logs(endpoint, token, context, samples, summary):
    events = [
        dict(context, **sample, dt=sample["startedAt"], event="deployment_health_check")
        for sample in samples
    ]
    events.append(dict(context, **summary, dt=summary["stoppedAt"], event="deployment_health_summary"))
    request = urllib.request.Request(
        endpoint, data=json.dumps(events).encode(),
        headers={"Authorization": f"Bearer {token}", "Content-Type": "application/json"},
        method="POST",
    )
    try:
        with urllib.request.build_opener(NoRedirect).open(request, timeout=15) as response:
            if response.status != 202:
                raise RuntimeError(f"Better Stack returned HTTP {response.status}")
    except urllib.error.HTTPError as error:
        raise RuntimeError(f"Better Stack returned HTTP {error.code}") from None
    except (OSError, ValueError):
        raise RuntimeError("Could not reach the Better Stack ingest endpoint") from None


def https_url(key):
    value = os.environ.get(key, "")
    parsed = urllib.parse.urlsplit(value)
    if parsed.scheme != "https" or not parsed.hostname or parsed.username is not None or parsed.fragment:
        raise ValueError(f"Configure {key} with an HTTPS URL")
    return value


def run():
    url = https_url("MONITOR_HEALTH_URL")
    endpoint = https_url("MONITOR_INGEST_URL")
    token = os.environ.get("MONITOR_SOURCE_TOKEN")
    if not token:
        raise ValueError("Configure MONITOR_SOURCE_TOKEN")
    context = {
        "environment": os.environ["MONITOR_ENVIRONMENT"],
        "runId": int(os.environ["GITHUB_RUN_ID"]),
        "runAttempt": int(os.environ["GITHUB_RUN_ATTEMPT"]),
        "deploymentCommitSha": os.environ["GITHUB_SHA"],
    }
    session = f"{context['runId']}-{context['runAttempt']}"
    samples, summary = measure(url, session, Path(os.environ["MONITOR_READY"]), DeploymentStatus())
    send_logs(endpoint, token, context, samples, summary)
    print(f"Sent {summary['total']} health checks to Better Stack ({summary['failures']} failures)")
    if summary["stopReason"] == "maximum_duration":
        raise RuntimeError("Measurement reached its time limit before post-deploy observation ended")


if __name__ == "__main__":
    try:
        if sys.argv[1:] == ["wait"]:
            wait_for_monitor()
        else:
            run()
    except (RuntimeError, ValueError) as error:
        sys.exit(str(error))
