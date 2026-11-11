"""실제 Docker/Nginx를 변경하지 않고 배포 사전 확인의 성공·실패 분기를 검증한다.

실행: python3 -m unittest discover -s backend/infra/dev/tests -v
"""

import os
from pathlib import Path
import subprocess
import tempfile
import unittest


SCRIPT = Path(__file__).resolve().parents[1] / "deploy.sh"


class DeployPreflightTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.directory = Path(self.temporary.name)
        self.config = self.directory / "nginx.conf"
        self.config.write_text("upstream yeobaek_app {\n server 127.0.0.1:8080;\n}\n")
        self.env = {
            **os.environ,
            "PATH": f"{self.directory}:{os.environ['PATH']}",
            "NGINX_CONFIG": str(self.config),
            "TEST_DETAILS": "running|yeobaek-dev|app-blue|127.0.0.1:8080;",
            "TEST_ACTIVE": "yeobaek-app-blue",
            "TEST_CONTAINERS": "yeobaek-app-blue\nyeobaek-mysql",
            "TEST_HEALTH_URL": "http://127.0.0.1:8080/health",
            "TEST_HTTP_STATUS": "200",
            "TEST_INSPECT_EXIT": "0",
            "TEST_LIST_EXIT": "0",
            "TEST_CURL_EXIT": "0",
        }
        self.command("docker", '''
case "$1 $2" in
  "container inspect")
    [[ "${!#}" == "$TEST_ACTIVE" ]] || exit 99
    printf '%s\\n' "$TEST_DETAILS"
    exit "$TEST_INSPECT_EXIT" ;;
  "container ls")
    printf '%s\\n' "$TEST_CONTAINERS"
    exit "$TEST_LIST_EXIT" ;;
  *) echo '허용하지 않은 Docker 명령' >&2; exit 99 ;;
esac
''')
        self.command("curl", '''
[[ "${!#}" == "$TEST_HEALTH_URL" ]] || exit 99
printf '%s' "$TEST_HTTP_STATUS"
exit "$TEST_CURL_EXIT"
''')

    def command(self, name, body):
        path = self.directory / name
        path.write_text("#!/usr/bin/env bash\nset -eu\n" + body)
        path.chmod(0o700)

    def run_check(self):
        return subprocess.run(
            ["bash", str(SCRIPT)], env=self.env, capture_output=True, text=True, timeout=5
        )

    def assert_stopped(self, message):
        result = self.run_check()
        self.assertEqual(1, result.returncode, result.stdout + result.stderr)
        self.assertIn(message, result.stderr)
        self.assertNotIn("사전 확인 완료", result.stdout)

    def test_blue_selects_green(self):
        result = self.run_check()
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("다음 배포 위치: Green (app-green, 호스트 포트 8081)", result.stdout)

    def test_green_selects_blue(self):
        self.config.write_text("upstream yeobaek_app {\n server 127.0.0.1:8081;\n}\n")
        self.env.update(
            TEST_DETAILS="running|yeobaek-dev|app-green|127.0.0.1:8081;",
            TEST_ACTIVE="yeobaek-app-green",
            TEST_CONTAINERS="yeobaek-app-green\nyeobaek-mysql",
            TEST_HEALTH_URL="http://127.0.0.1:8081/health",
        )
        result = self.run_check()
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("다음 배포 위치: Blue (app-blue, 호스트 포트 8080)", result.stdout)

    def test_leftover_container_stops_deployment(self):
        self.env["TEST_CONTAINERS"] += "\nyeobaek-app-green"
        self.assert_stopped("yeobaek-app-green 컨테이너가 남아 있습니다")

    def test_docker_list_failure_is_not_an_empty_slot(self):
        self.env.update(TEST_CONTAINERS="", TEST_LIST_EXIT="1")
        self.assert_stopped("컨테이너 목록을 확인하지 못했습니다")

    def test_inspect_failure_stops_deployment(self):
        self.env["TEST_INSPECT_EXIT"] = "1"
        self.assert_stopped("상태를 확인하지 못했습니다")

    def test_stopped_current_app_is_rejected(self):
        self.env["TEST_DETAILS"] = "exited|yeobaek-dev|app-blue|127.0.0.1:8080;"
        self.assert_stopped("실행 상태가 running이 아닙니다")

    def test_other_compose_project_is_rejected(self):
        self.env["TEST_DETAILS"] = "running|yeobaek-prod|app-blue|127.0.0.1:8080;"
        self.assert_stopped("예상한 개발용 Compose 서비스가 아닙니다")

    def test_renamed_container_with_legacy_service_is_rejected(self):
        self.env["TEST_DETAILS"] = "running|yeobaek-dev|app|127.0.0.1:8080;"
        self.assert_stopped("예상한 개발용 Compose 서비스가 아닙니다")

    def test_wrong_port_binding_is_rejected(self):
        self.env["TEST_DETAILS"] = "running|yeobaek-dev|app-blue|127.0.0.1:8081;"
        self.assert_stopped("포트 연결이 예상과 다릅니다")

    def test_failed_or_redirected_health_is_rejected(self):
        for status in ("503", "302"):
            with self.subTest(status=status):
                self.env["TEST_HTTP_STATUS"] = status
                self.assert_stopped("응답이 200이 아닙니다")

    def test_health_timeout_stops_deployment(self):
        self.env["TEST_CURL_EXIT"] = "28"
        self.assert_stopped("/health 요청이 실패했습니다")

    def test_ambiguous_or_unsupported_upstream_is_rejected(self):
        for block in (
            "upstream other {\n server 127.0.0.1:8080;\n}\n",
            "upstream yeobaek_app {\n server 127.0.0.1:8082;\n}\n",
            "upstream yeobaek_app {\n server 127.0.0.1:8080;\n server 127.0.0.1:8081;\n}\n",
            "upstream yeobaek_app {\n server 127.0.0.1:8080;\n",
        ):
            with self.subTest(block=block):
                self.config.write_text(block)
                self.assert_stopped("upstream에")

    def test_unreadable_config_stops_deployment(self):
        self.config.unlink()
        self.assert_stopped("Nginx 설정 파일을 읽을 수 없습니다")


if __name__ == "__main__":
    unittest.main()
