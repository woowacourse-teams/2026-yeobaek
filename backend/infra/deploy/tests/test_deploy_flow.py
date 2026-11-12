"""가짜 Docker/Nginx 명령으로 실제 deploy.sh의 전환·복구·정리 분기를 실행한다."""

import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest


SCRIPT = Path(__file__).resolve().parents[1] / "deploy.sh"
OLD_SHA = "a" * 40
NEW_SHA = "b" * 40

FAKE_COMMAND = r'''
import json, os, pathlib, shutil, sys
name = pathlib.Path(sys.argv[0]).name
args = sys.argv[1:]
root = pathlib.Path(os.environ["TEST_ROOT"])
state_path = root / "state.json"
state = json.loads(state_path.read_text())
scenario = os.environ.get("TEST_SCENARIO", "success")
with (root / "calls.jsonl").open("a") as f:
    f.write(json.dumps([name] + args) + "\n")
config = root / "nginx.conf"
old = "a" * 40
new = "b" * 40
active = state.get("active", "yeobaek-app-blue")
next_container = "yeobaek-app-green" if active == "yeobaek-app-blue" else "yeobaek-app-blue"
next_service = "app-green" if active == "yeobaek-app-blue" else "app-blue"
project = os.environ.get("TEST_COMPOSE_PROJECT", "yeobaek-dev")

def end(code=0, output=""):
    state_path.write_text(json.dumps(state))
    if output: print(output)
    sys.exit(code)

if name == "flock":
    end(1 if scenario == "locked" else 0)
if name == "timeout":
    os.execvp(args[1], args[1:])
if name == "curl":
    sys.stdout.write("200")
    end()
if name == "python3":
    action = args[1]
    if action == "health":
        url, sha = args[2:4]
        code = 0
        if "8081" in url and scenario == "not-ready": code = 1
        if url.startswith("https") and sha == new and scenario in ("public-fail", "rollback-fail", "rollback-drain-fail"):
            code = 1
        if url.startswith("https") and sha == old and state["reloads"] >= 2 and scenario == "rollback-fail": code = 1
        observed = old if sha == "-" else sha
        import importlib.util
        spec = importlib.util.spec_from_file_location("support", args[0])
        support = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(support)
        support.record_health(args[5], args[6], {"url": url, "status": "failed" if code else "passed",
                                               "expected_commit_sha": sha, "observed_commit_sha": observed})
        end(code, observed if code == 0 else "")
    if action == "snapshot":
        pathlib.Path(args[2]).write_text("{}")
        end()
    if action == "wait-workers":
        if "workers-before-switch" in args[2] and scenario == "drain-fail": end(1)
        if "nginx-workers-before-restore" in args[2] and scenario == "rollback-drain-fail": end(1)
        pathlib.Path(args[4]).write_text("test worker exit log")
        end()
    if action == "check-timeout": end()
    if action in ("prune-records", "metadata", "record-shutdown", "cleanup-temporary"):
        if action == "prune-records" and scenario == "prune-fail": end(1, "test cleanup failure")
        os.execv(sys.executable, [sys.executable] + args)
if name == "sudo":
    args = args[1:]
    if args[:2] == ["nginx", "-T"]:
        end(0, "# configuration file " + str(config) + ":\n" + config.read_text())
    if args[:2] == ["nginx", "-t"]:
        end(1 if "8081" in config.read_text() and scenario == "invalid-config" else 0)
    if args[:3] == ["nginx", "-s", "reload"]:
        state["reloads"] += 1
        end(1 if scenario == "reload-fail" and state["reloads"] == 1 else 0)
    if args[0] == "cp": shutil.copy2(args[-2], args[-1]); end()
    if args[0] == "tee": pathlib.Path(args[1]).write_text(sys.stdin.read()); end()
    if args[0] == "mv": os.replace(args[-2], args[-1]); end()
    if args[0] == "rm": pathlib.Path(args[-1]).unlink(missing_ok=True); end()
if name == "docker":
    if args[0] == "compose":
        if "up" in args:
            assert "--no-deps" in args and "--pull" in args and args[-1] == next_service
            state["containers"][next_container] = "running"
        if "pull" in args and scenario == "pull-fail": end(1)
        end()
    if args[:2] == ["image", "inspect"]: end(0, "sha256:" + "c" * 64)
    if args[:2] == ["container", "ls"]: end(0, "\n".join(state["containers"]))
    container = args[-1]
    if args[:2] == ["container", "inspect"]:
        if container not in state["containers"]: end(1)
        status = state["containers"][container]
        template = args[3]
        if "json .State" in template: end(0, json.dumps({"Status": status}))
        if template == "{{.Image}}": end(0, "sha256:" + "d" * 64)
        if "State.ExitCode" in template:
            code = {"forced-stop": 137, "sigterm-stop": 143, "oom-stop": 143, "error-stop": 1}.get(scenario, 0)
            oom = "true" if scenario == "oom-stop" else "false"
            end(0, f"{status}|{'true' if status == 'running' else 'false'}|{code}|{oom}")
        if active == "yeobaek-app-blue": end(0, f"running|{project}|app-blue|127.0.0.1:8080;")
        end(0, f"running|{project}|app-green|127.0.0.1:8081;")
    if args[:2] == ["container", "logs"]: end(0, "test application log")
    if args[:2] == ["container", "stop"]:
        if scenario == "stop-fail": end(1)
        state["containers"][container] = "exited"
        end()
    if args[:2] == ["container", "rm"]:
        assert state["containers"][container] == "exited"
        if scenario == "remove-fail": end(1)
        del state["containers"][container]
        end()
end(99, "Unexpected mock command: " + name + " " + repr(args))
'''


class DeployFlowTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        binary = self.root / "bin"
        binary.mkdir()
        fake = binary / "fake"
        fake.write_text(f"#!{sys.executable}\n" + FAKE_COMMAND)
        fake.chmod(0o700)
        for name in ("docker", "sudo", "curl", "python3", "timeout", "flock"):
            (binary / name).symlink_to(fake)
        self.config = self.root / "nginx.conf"
        self.original = "upstream yeobaek_app {\n    server 127.0.0.1:8080;\n}\n"
        self.config.write_text(self.original)
        (self.root / "docker-compose.yml").touch()
        (self.root / ".env").touch()
        (self.root / "state.json").write_text(json.dumps({
            "containers": {"yeobaek-app-blue": "running"}, "reloads": 0,
        }))
        self.summary = self.root / "actions-summary.md"
        self.env = {**os.environ, "PATH": f"{binary}:{os.environ['PATH']}",
                    "TEST_ROOT": str(self.root), "DEPLOY_DIR": str(self.root),
                    "NGINX_CONFIG": str(self.config), "WORKER_WAIT_SECONDS": "1",
                    "GITHUB_ACTIONS": "true", "GITHUB_STEP_SUMMARY": str(self.summary)}

    def deploy(self, scenario="success"):
        self.env["TEST_SCENARIO"] = scenario
        result = subprocess.run(["bash", str(SCRIPT), "example/app:" + NEW_SHA, NEW_SHA],
                                env=self.env, text=True, capture_output=True, timeout=30)
        self.state = json.loads((self.root / "state.json").read_text())
        self.calls = [json.loads(line) for line in (self.root / "calls.jsonl").read_text().splitlines()]
        self.output = result.stdout + result.stderr
        return result.returncode

    def stopped(self):
        return [call[-1] for call in self.calls if call[:3] == ["docker", "container", "stop"]]

    def test_production_uses_its_project_domain_and_nginx_staging_file(self):
        domain = "https://yeobaek.duckdns.org/health"
        self.env.update(COMPOSE_PROJECT_NAME="yeobaek", TEST_COMPOSE_PROJECT="yeobaek",
                        HEALTH_URL=domain, DEPLOY_LABEL="운영")
        self.assertEqual(0, self.deploy(), self.output)
        self.assertIn("## 🟢 운영 배포 완료", self.summary.read_text())
        compose = [call for call in self.calls if call[:2] == ["docker", "compose"]]
        self.assertTrue(compose)
        self.assertTrue(all(call[call.index("-p") + 1] == "yeobaek" for call in compose))
        self.assertTrue(any(call[1:3] == ["-n", "mv"] and
                            call[-2] == str(self.root / ".yeobaek-deploy.tmp")
                            for call in self.calls if call[0] == "sudo"))
        domain_checks = [call[3] for call in self.calls if call[0] == "python3" and
                         call[2] == "health" and call[3].startswith("https://")]
        self.assertEqual([domain, domain], domain_checks)

    def test_production_legacy_app_fails_preflight_before_switching(self):
        self.env.update(COMPOSE_PROJECT_NAME="yeobaek", DEPLOY_LABEL="운영")
        (self.root / "state.json").write_text(json.dumps({
            "containers": {"yeobaek-app": "running"}, "reloads": 0,
        }))
        self.assertEqual(1, self.deploy(), self.output)
        self.assertEqual({"yeobaek-app": "running"}, self.state["containers"])
        self.assertEqual(0, self.state["reloads"])
        self.assertEqual(self.original, self.config.read_text())
        self.assertFalse(any(call[:2] == ["docker", "compose"] for call in self.calls))

    def test_success_drains_before_stopping_old_app(self):
        self.assertEqual(0, self.deploy(), self.output)
        summary = self.summary.read_text()
        self.assertIn("## 🟢 개발 배포 완료", summary)
        self.assertNotIn("🟠", summary)
        self.assertIn(NEW_SHA, summary)
        self.assertEqual({"yeobaek-app-green": "running"}, self.state["containers"])
        self.assertIn("8081", self.config.read_text())
        drain = next(i for i, c in enumerate(self.calls) if c[0] == "python3" and c[2] == "wait-workers")
        stop = next(i for i, c in enumerate(self.calls) if c[:3] == ["docker", "container", "stop"])
        self.assertLess(drain, stop)

    def test_readiness_failure_leaves_blue_and_removes_green(self):
        self.assertEqual(1, self.deploy("not-ready"), self.output)
        self.assertEqual(["yeobaek-app-green"], self.stopped())
        self.assertEqual(self.original, self.config.read_text())
        self.assertEqual(0, self.state["reloads"])

    def test_green_to_blue_deployment(self):
        self.config.write_text(self.original.replace("8080", "8081"))
        (self.root / "state.json").write_text(json.dumps({
            "active": "yeobaek-app-green", "containers": {"yeobaek-app-green": "running"}, "reloads": 0,
        }))
        self.assertEqual(0, self.deploy(), self.output)
        self.assertEqual({"yeobaek-app-blue": "running"}, self.state["containers"])
        self.assertIn("8080", self.config.read_text())
        self.assertEqual(["yeobaek-app-green"], self.stopped())

    def test_switch_failures_restore_blue_and_remove_only_green(self):
        self.assertEqual(1, self.deploy("public-fail"), self.output)
        self.assertEqual(self.original, self.config.read_text())
        self.assertEqual(["yeobaek-app-green"], self.stopped())
        self.assertEqual(2, self.state["reloads"])
        record = next((self.root / "deployment" / "records").iterdir())
        rollback = json.loads((record / "deployment.json").read_text())["rollback"]
        self.assertEqual("recovered", rollback["status"])
        for key in ("started_at", "traffic_restored_at", "completed_at"):
            self.assertTrue(rollback[key].endswith("+09:00"))
        self.assertTrue((record / "rollback" / "nginx-workers-before-restore.json").exists())
        self.assertTrue((record / "rollback" / "nginx-worker-drain.log").exists())
        self.assertIn("restored_app_via_domain", json.loads((record / "health-checks.json").read_text()))

    def test_invalid_config_rolls_back(self):
        self.assertEqual(1, self.deploy("invalid-config"), self.output)
        self.assertEqual(self.original, self.config.read_text())
        self.assertEqual(["yeobaek-app-green"], self.stopped())

    def test_reload_failure_rolls_back_even_if_command_may_have_applied(self):
        self.assertEqual(1, self.deploy("reload-fail"), self.output)
        self.assertEqual(2, self.state["reloads"])
        self.assertEqual(self.original, self.config.read_text())

    def test_unverified_recovery_retains_both_apps(self):
        self.assertEqual(1, self.deploy("rollback-fail"), self.output)
        self.assertEqual([], self.stopped())
        self.assertEqual(2, len(self.state["containers"]))

    def test_rollback_drain_failure_keeps_green(self):
        self.assertEqual(1, self.deploy("rollback-drain-fail"), self.output)
        self.assertEqual([], self.stopped())
        self.assertEqual(self.original, self.config.read_text())

    def test_drain_timeout_keeps_green_route_and_blue_container(self):
        self.assertEqual(1, self.deploy("drain-fail"), self.output)
        self.assertIn("## 🔴 개발 배포 실패", self.summary.read_text())
        self.assertIn("**실패 이유:**", self.summary.read_text())
        self.assertIn("::error::", self.output)
        self.assertEqual([], self.stopped())
        self.assertEqual(2, len(self.state["containers"]))
        self.assertEqual(1, self.state["reloads"])
        self.assertIn("8081", self.config.read_text())

    def test_forced_but_confirmed_stop_is_success_with_warning(self):
        self.assertEqual(0, self.deploy("forced-stop"), self.output)
        self.assertIn("강제 종료 가능성", self.output)
        self.assertIn("## 🟠 개발 배포 완료", self.summary.read_text())
        self.assertIn("강제 종료 가능성", self.summary.read_text())
        self.assertIn("::warning::", self.output)
        self.assertNotIn("::error::", self.output)

    def test_confirmed_sigterm_stop_is_success_without_warning(self):
        self.assertEqual(0, self.deploy("sigterm-stop"), self.output)
        self.assertIn("## 🟢 개발 배포 완료", self.summary.read_text())
        self.assertNotIn("::warning::", self.output)
        self.assertIn("SIGTERM 전송 후 종료 확인(exit=143)", self.output)
        record = next((self.root / "deployment" / "records").iterdir())
        shutdown = json.loads((record / "containers" / "yeobaek-app-blue" / "shutdown.json").read_text())
        self.assertEqual(143, shutdown["exit_code"])
        self.assertFalse(shutdown["running"])
        self.assertFalse(shutdown["oom_killed"])
        stop = next(c for c in self.calls if c[:3] == ["docker", "container", "stop"])
        self.assertEqual("SIGTERM", stop[stop.index("--signal") + 1])

    def test_oom_is_warning_even_with_sigterm_exit_code(self):
        self.assertEqual(0, self.deploy("oom-stop"), self.output)
        self.assertIn("## 🟠 개발 배포 완료", self.summary.read_text())
        self.assertIn("::warning::", self.output)
        self.assertIn("exit=143, OOM=true", self.output)

    def test_other_nonzero_exit_code_remains_warning(self):
        self.assertEqual(0, self.deploy("error-stop"), self.output)
        self.assertIn("## 🟠 개발 배포 완료", self.summary.read_text())
        self.assertIn("::warning::", self.output)
        self.assertIn("종료 코드=1", self.output)

    def test_unconfirmed_stop_fails_without_rollback(self):
        self.assertEqual(1, self.deploy("stop-fail"), self.output)
        self.assertIn("## 🔴 개발 배포 실패", self.summary.read_text())
        self.assertIn("### 🟠 경고", self.summary.read_text())
        self.assertEqual(2, len(self.state["containers"]))
        self.assertEqual(1, self.state["reloads"])

    def test_container_removal_failure_is_cleanup_failure(self):
        self.assertEqual(1, self.deploy("remove-fail"), self.output)
        self.assertEqual("exited", self.state["containers"]["yeobaek-app-blue"])
        self.assertIn("8081", self.config.read_text())

    def test_pull_failure_does_not_touch_current_app(self):
        self.assertEqual(1, self.deploy("pull-fail"), self.output)
        self.assertEqual([], self.stopped())
        self.assertEqual(self.original, self.config.read_text())

    def test_concurrent_deployment_stops_before_any_change(self):
        self.assertEqual(1, self.deploy("locked"), self.output)
        self.assertEqual([["flock", "-n", "9"]], self.calls)

    def test_retention_runs_after_failed_deployment_and_keeps_its_result(self):
        records = self.root / "deployment" / "records"
        records.mkdir(parents=True)
        for day in range(1, 23):
            (records / f"{OLD_SHA}__202501{day:02d}-000000-KST__aaaaaa").mkdir()
        self.assertEqual(1, self.deploy("public-fail"), self.output)
        self.assertEqual(20, len(list(records.iterdir())))
        self.assertFalse((records / f"{OLD_SHA}__20250103-000000-KST__aaaaaa").exists())
        self.assertTrue((records / f"{OLD_SHA}__20250104-000000-KST__aaaaaa").exists())
        results = list(records.glob("*/deployment.json"))
        self.assertEqual(1, len(results))
        self.assertEqual(1, json.loads(results[0].read_text())["result"]["exit_code"])

    def test_retention_failure_does_not_change_successful_deployment(self):
        self.assertEqual(0, self.deploy("prune-fail"), self.output)
        self.assertIn("오래된 배포 기록 정리 실패", self.output)
        self.assertIn("## 🟠 개발 배포 완료", self.summary.read_text())
        self.assertIn("오래된 배포 기록 정리 실패", self.summary.read_text())
        result = next((self.root / "deployment" / "records").glob("*/deployment.json"))
        self.assertEqual(0, json.loads(result.read_text())["result"]["exit_code"])

    def test_record_layout_groups_metadata_health_and_containers(self):
        self.assertEqual(0, self.deploy(), self.output)
        root = self.root / "deployment"
        self.assertTrue((root / "deploy.lock").exists())
        record = next((root / "records").iterdir())
        self.assertTrue(record.name.startswith(NEW_SHA + "__"))
        self.assertIn("-KST__", record.name)
        data = json.loads((record / "deployment.json").read_text())
        self.assertEqual(NEW_SHA, data["target"]["commit_sha"])
        self.assertEqual(OLD_SHA, data["previous"]["commit_sha"])
        self.assertIn("배포 시작 시각", data["_descriptions"]["started_at"])
        self.assertTrue(data["started_at"].endswith("+09:00"))
        self.assertTrue(data["finished_at"].endswith("+09:00"))
        self.assertEqual({"active_app_local", "active_app_via_domain", "new_app_local", "new_app_via_domain"},
                         set(json.loads((record / "health-checks.json").read_text())))
        self.assertTrue((record / "nginx" / "config-before-deploy.conf").exists())
        self.assertTrue((record / "nginx" / "worker-drain.log").exists())
        self.assertTrue((record / "compose-image-override.yml").exists())
        container = record / "containers" / "yeobaek-app-blue"
        self.assertTrue((container / "application.log").exists())
        self.assertFalse(json.loads((container / "shutdown.json").read_text())["running"])
        self.assertFalse((record / "started-at").exists())

    def test_stale_temporary_files_are_cleaned_after_lock_before_new_deployment(self):
        root = self.root / "deployment"
        record = root / "records" / f"{OLD_SHA}__20250101-000000-KST__aaaaaa"
        record.mkdir(parents=True)
        temporary = record / ".deployment.json.tmp"
        temporary.write_text("incomplete")
        nginx_temp = self.root / ".yeobaek-dev-deploy.tmp"
        nginx_temp.write_text("incomplete")
        unrelated = self.root / "other.tmp"
        unrelated.write_text("keep")
        self.assertEqual(0, self.deploy(), self.output)
        self.assertFalse(temporary.exists())
        self.assertFalse(nginx_temp.exists())
        self.assertEqual("keep", unrelated.read_text())
        lock = next(i for i, c in enumerate(self.calls) if c[0] == "flock")
        cleanup = next(i for i, c in enumerate(self.calls) if c[0] == "python3" and c[2] == "cleanup-temporary")
        start = next(i for i, c in enumerate(self.calls) if c[0] == "python3" and c[2] == "metadata" and c[4] == "started")
        self.assertLess(lock, cleanup)
        self.assertLess(cleanup, start)


if __name__ == "__main__":
    unittest.main()
