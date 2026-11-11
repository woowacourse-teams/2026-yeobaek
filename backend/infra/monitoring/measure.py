"""One probe per second on a GitHub-hosted runner; commands live in artifacts."""
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
import urllib.parse

from github_api import GitHub

WORKFLOW = ".github/workflows/deployment-monitor.yml"
INTERVAL = 1
REQUEST_TIMEOUT = 3
MAX_DURATION = 1800
AFTER_DEPLOYMENT = 300


def timestamp():
    return dt.datetime.now(dt.timezone.utc).isoformat(timespec="milliseconds")


def epoch(value):
    return dt.datetime.fromisoformat(value.replace("Z", "+00:00")).timestamp()


def write_json(path, value):
    temporary = path.with_suffix(".tmp")
    temporary.write_text(json.dumps(value, ensure_ascii=False), encoding="utf-8")
    temporary.replace(path)


def settings():
    environment = os.environ["MONITOR_ENVIRONMENT"]
    session = os.environ["MONITOR_SESSION"]
    if environment not in ("dev", "prod") or not re.fullmatch(r"[a-f0-9-]{36}", session):
        raise ValueError("Invalid environment/session")
    url = os.environ.get(f"MONITOR_{environment.upper()}_HEALTH_URL", "")
    parsed = urllib.parse.urlparse(url)
    if parsed.scheme != "https" or not parsed.hostname or parsed.username or parsed.fragment:
        raise ValueError("Configure an HTTPS MONITOR health URL without credentials")
    return environment, session, url


def probe(url, session, sequence, ready=None):
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
            if ready is not None:
                ready.set()
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
    return {"total": len(ordered), "successes": len(ordered) - failures, "failures": failures,
            "outages": outages, "versions": versions,
            "estimatedRecoveredSeconds": round(sum(item["estimatedSeconds"] or 0 for item in outages), 3),
            "recoveryUnconfirmed": start is not None}


def allowed_deployment(run, environment):
    branch = "develop" if environment == "dev" else "main"
    configured = os.environ.get(f"MONITOR_{environment.upper()}_DEPLOY_WORKFLOWS", "")
    names = configured.split(",") if configured else [f"backend-{environment}.yml"]
    return run["head_branch"] == branch and run["path"].split("@")[0] in [
        ".github/workflows/" + name.strip() for name in names]


class Controls:
    def __init__(self, github, environment, session):
        self.github = github
        self.environment = environment
        self.session = session
        self.seen = set()
        self.links = {}
        self.stop = False
        self.error_count = 0
        self.last_error_at = None
        self.last_checked_at = None
        self.deadline = None
        self.lock = threading.Lock()

    def refresh(self):
        try:
            artifacts = sorted(self.github.artifacts(), key=lambda item: item["id"])
            prefix = f"ym-command-{self.environment}-{self.session}-"
            for artifact in artifacts:
                if artifact["expired"] or not artifact["name"].startswith(prefix) or artifact["id"] in self.seen:
                    continue
                source = self.github.request(f'/actions/runs/{artifact["workflow_run"]["id"]}')
                if source["path"].split("@")[0] != WORKFLOW or not source["display_title"].startswith(
                        f"monitor:{self.environment}:{self.session}:"):
                    raise ValueError("Invalid command source")
                command = self.github.artifact_json(artifact["id"], "command.json")
                if command["command"] == "stop":
                    with self.lock:
                        self.stop = True
                elif command["command"] == "link":
                    run_id, attempt = int(command["deploymentRunId"]), int(command["attempt"])
                    run = self.github.request(f"/actions/runs/{run_id}/attempts/{attempt}")
                    if not allowed_deployment(run, self.environment):
                        raise ValueError("Invalid deployment workflow")
                    with self.lock:
                        self.links[(run_id, attempt)] = run
                        self.deadline = None
                else:
                    raise ValueError("Invalid command")
                self.seen.add(artifact["id"])
            with self.lock:
                previous = dict(self.links)
                keys = list(previous)
            refreshed, completed = {}, []
            for run_id, attempt in keys:
                cached = previous[(run_id, attempt)]
                if cached.get("measurementCompletedAt"):
                    refreshed[(run_id, attempt)] = cached
                    completed.append(epoch(cached["measurementCompletedAt"]))
                    continue
                run = self.github.request(f"/actions/runs/{run_id}/attempts/{attempt}")
                refreshed[(run_id, attempt)] = run
                run["measurementStartedAt"] = run.get("run_started_at") or run.get("created_at")
                run["measurementCompletedAt"] = None
                if run["status"] == "completed":
                    jobs = list(self.github.pages(
                        f"/actions/runs/{run_id}/attempts/{attempt}/jobs", "jobs"))
                    starts = [epoch(job["started_at"]) for job in jobs if job.get("started_at")]
                    if starts:
                        run["measurementStartedAt"] = dt.datetime.fromtimestamp(min(starts), dt.timezone.utc).isoformat()
                    times = [epoch(job["completed_at"]) for job in jobs if job.get("completed_at")]
                    end = max(times) if times else epoch(run["updated_at"])
                    run["measurementCompletedAt"] = dt.datetime.fromtimestamp(end, dt.timezone.utc).isoformat()
                    completed.append(end)
            with self.lock:
                self.links.update(refreshed)
                self.deadline = max(completed) + AFTER_DEPLOYMENT if keys and len(completed) == len(keys) else None
                self.last_checked_at = timestamp()
        except (OSError, ValueError, KeyError):
            # Unknown deployment state must not be interpreted as completed.
            with self.lock:
                self.deadline = None
                self.error_count += 1
                self.last_error_at = timestamp()

    def poll(self, finished):
        while not finished.is_set():
            self.refresh()
            finished.wait(10)

    def snapshot(self):
        with self.lock:
            return self.stop, self.deadline, {
                "deployments": [{key: run.get(key) for key in (
                    "id", "run_attempt", "head_sha", "status", "conclusion", "html_url", "created_at",
                    "measurementStartedAt", "measurementCompletedAt")}
                    for run in self.links.values()],
                "controlErrors": self.error_count, "lastControlErrorAt": self.last_error_at,
                "lastControlCheckAt": self.last_checked_at,
            }


def measure():
    environment, session, url = settings()
    directory = Path(os.environ["MONITOR_OUTPUT"])
    directory.mkdir(parents=True, exist_ok=True)
    github = GitHub()
    controls = Controls(github, environment, session)
    finished = threading.Event()
    thread = threading.Thread(target=controls.poll, args=(finished,), daemon=True)
    futures, samples = [], []
    started, monotonic_start = timestamp(), time.monotonic()
    state = {"session": session, "environment": environment, "target": url,
             "startedAt": started, "maxDurationSeconds": MAX_DURATION,
             "intervalSeconds": INTERVAL, "requestTimeoutSeconds": REQUEST_TIMEOUT}
    reason, fatal, skipped = "maximum_duration", None, 0
    try:
        with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
            # Submit before publishing readiness; the browser never treats job setup as measuring.
            ready = threading.Event()
            futures.append(pool.submit(probe, url, session, 0, ready))
            if not ready.wait(5):
                raise RuntimeError("First probe did not start")
            write_json(directory / "state.json", state)
            thread.start()
            next_sequence = 1
            while time.monotonic() - monotonic_start < MAX_DURATION:
                stop, deadline, _ = controls.snapshot()
                if stop or (deadline is not None and time.time() >= deadline):
                    reason = "manual" if stop else "deployment_finished"
                    break
                elapsed = time.monotonic() - monotonic_start
                if elapsed >= next_sequence * INTERVAL:
                    current_sequence = int(elapsed // INTERVAL)
                    skipped += max(0, current_sequence - next_sequence)
                    futures.append(pool.submit(probe, url, session, current_sequence))
                    next_sequence = current_sequence + 1
                for future in list(futures):
                    if future.done():
                        samples.append(future.result())
                        futures.remove(future)
                time.sleep(0.05)
            sending_stopped_at = timestamp()
            for future in futures:
                samples.append(future.result())
    except Exception as error:  # Preserve partial evidence even if the runner script fails.
        reason, fatal, sending_stopped_at = "measurement_error", type(error).__name__, timestamp()
    finally:
        finished.set()
        _, _, control_state = controls.snapshot()
        ordered = sorted(samples, key=lambda sample: sample["sequence"])
        result = {**state, **control_state, "sendingStoppedAt": sending_stopped_at,
                  "finishedAt": timestamp(), "stopReason": reason, "fatalError": fatal,
                  "missedScheduledRequests": skipped, "summary": summarize(ordered), "samples": ordered}
        write_json(directory / "result.json", result)
    return 1 if fatal else 0


def guard():
    environment, session, _ = settings()
    github = GitHub()
    own_id = int(os.environ["GITHUB_RUN_ID"])
    own = github.request(f"/actions/runs/{own_id}")
    if int(os.environ["GITHUB_RUN_ATTEMPT"]) != 1:
        raise ValueError("Start a new measurement instead of rerunning an old session")
    runs = github.pages("/actions/workflows/deployment-monitor.yml/runs?event=workflow_dispatch", "workflow_runs")
    for run in runs:
        if run["id"] == own_id or not run["display_title"].startswith(f"monitor:{environment}:"):
            continue
        if not run["display_title"].endswith(":start"):
            continue
        # A queued duplicate must not begin an unrelated measurement after the current one finishes.
        if run["id"] < own_id and (run["status"] != "completed" or run["updated_at"] >= own["created_at"]):
            raise ValueError("Another measurement was active when this start was requested")
        if run["display_title"] == f"monitor:{environment}:{session}:start":
            raise ValueError("Session already exists")


def command():
    environment, session, _ = settings()
    github = GitHub()
    run_id = int(os.environ["MONITOR_MEASUREMENT_RUN_ID"])
    run = github.request(f"/actions/runs/{run_id}")
    if run["path"].split("@")[0] != WORKFLOW or run["display_title"] != f"monitor:{environment}:{session}:start":
        raise ValueError("Invalid measurement")
    if run["status"] == "completed":
        raise ValueError("Measurement already finished")
    action = os.environ["MONITOR_COMMAND"]
    payload = {"command": action}
    if action == "link":
        deployment_id = int(os.environ["MONITOR_DEPLOYMENT_RUN_ID"])
        attempt = int(os.environ["MONITOR_DEPLOYMENT_ATTEMPT"])
        deployment = github.request(f"/actions/runs/{deployment_id}/attempts/{attempt}")
        if not allowed_deployment(deployment, environment):
            raise ValueError("Invalid deployment")
        payload.update(deploymentRunId=deployment_id, attempt=attempt)
    elif action != "stop":
        raise ValueError("Invalid command")
    directory = Path(os.environ["MONITOR_OUTPUT"])
    directory.mkdir(parents=True, exist_ok=True)
    write_json(directory / "command.json", payload)


def cleanup():
    environment, session, _ = settings()
    github = GitHub()
    artifacts = github.artifacts()
    prefix = f"ym-result-{environment}-"
    results = sorted([a for a in artifacts if not a["expired"] and a["name"].startswith(prefix)],
                     key=lambda a: a["created_at"], reverse=True)
    if not any(a["name"] == prefix + session for a in results):
        raise ValueError("New result not uploaded; refusing cleanup")
    for result in results[20:]:
        old_session = result["name"][len(prefix):]
        if not re.fullmatch(r"[a-f0-9-]{36}", old_session):
            continue
        for artifact in artifacts:
            if artifact["name"] in (result["name"], f"ym-state-{environment}-{old_session}") or artifact[
                    "name"].startswith(f"ym-command-{environment}-{old_session}-"):
                github.request(f'/actions/artifacts/{artifact["id"]}', "DELETE")


if __name__ == "__main__":
    functions = {"guard": guard, "command": command, "measure": measure, "cleanup": cleanup}
    sys.exit(functions[sys.argv[1]]())
