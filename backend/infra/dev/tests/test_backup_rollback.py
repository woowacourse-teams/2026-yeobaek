"""서비스 이름 전환 전후의 설정 백업·복구를 가짜 Docker로 확인한다."""

import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest


SCRIPTS = Path(__file__).resolve().parents[1]
FAKE_DOCKER = r'''
import json, os, pathlib, sys
args = sys.argv[1:]
root = pathlib.Path(os.environ["DEPLOY_DIR"])
service = os.environ["TEST_SERVICE"]
with (root / "commands.jsonl").open("a") as target:
    target.write(json.dumps(args) + "\n")
if args[0] == "inspect":
    labels = {"com.docker.compose.project.working_dir": str(root),
              "com.docker.compose.project": "yeobaek-dev",
              "com.docker.compose.service": service}
    if "-f" not in args:
        print(json.dumps([{"Config": {"Labels": labels}}]))
    elif args[2] == "{{.Image}}":
        print("sha256:test-image")
    else:
        print(next(value for key, value in labels.items() if '"' + key + '"' in args[2]))
elif args[:2] == ["image", "inspect"]:
    print("[]")
elif args[:2] == ["image", "save"]:
    print("test image archive")
elif args[:2] == ["image", "load"]:
    assert sys.stdin.read().strip() == "test image archive"
elif args[:2] == ["image", "tag"]:
    pass
elif args[0] == "compose":
    if "config" in args:
        print("services:\n  " + service + ":\n    image: test")
    elif "ps" in args:
        print("mysql-id" if "-q" in args else "test containers")
    elif "up" in args:
        assert args[-1] in (service, "otel-collector")
        assert "--no-deps" in args
    else:
        sys.exit(99)
else:
    sys.exit(99)
'''


class BackupRollbackTest(unittest.TestCase):
    def test_backup_and_restore_use_original_service_name(self):
        for service, port in (("app", 8080), ("app-blue", 8080), ("app-green", 8081)):
            with self.subTest(service=service), tempfile.TemporaryDirectory() as temporary:
                root = Path(temporary).resolve()
                binary = root / "bin"
                binary.mkdir()
                docker = binary / "docker"
                docker.write_text(f"#!{sys.executable}\n" + FAKE_DOCKER)
                docker.chmod(0o700)
                curl = binary / "curl"
                curl.write_text('#!/usr/bin/env bash\nprintf "%s\\n" "${!#}" > "$DEPLOY_DIR/health-url"\n')
                curl.chmod(0o700)
                for filename in ("docker-compose.yml", "otel-collector.yaml", ".env", "otel.env"):
                    (root / filename).write_text("test config\n")
                env = {**os.environ, "PATH": f"{binary}:{os.environ['PATH']}",
                       "DEPLOY_DIR": str(root), "TEST_SERVICE": service,
                       "APP_CONTAINER": "yeobaek-" + service}
                backup_run = subprocess.run(["bash", str(SCRIPTS / "backup.sh")], env=env,
                                            capture_output=True, text=True, timeout=10)
                self.assertEqual(0, backup_run.returncode, backup_run.stderr)
                backup = Path(backup_run.stdout.strip())
                self.assertIn(f"  {service}:\n", (backup / "rollback-images.yml").read_text())
                restore = subprocess.run(["bash", str(SCRIPTS / "rollback.sh"), str(backup)],
                                         env=env, capture_output=True, text=True, timeout=10)
                self.assertEqual(0, restore.returncode, restore.stderr)
                commands = [json.loads(line) for line in (root / "commands.jsonl").read_text().splitlines()]
                restored = [args[-1] for args in commands if args[0] == "compose" and "up" in args]
                self.assertEqual(["otel-collector", service], restored)
                self.assertEqual(f"http://127.0.0.1:{port}/health\n", (root / "health-url").read_text())


if __name__ == "__main__":
    unittest.main()
