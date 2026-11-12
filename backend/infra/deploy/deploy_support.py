"""deploy.sh에서 사용하는 시간 제한 헬스체크와 Nginx worker 확인. 외부 패키지 불필요."""

import json
from datetime import datetime, timedelta, timezone
from pathlib import Path
import re
import shutil
import subprocess
import sys
import time


RECORD_NAME = re.compile(r"[0-9a-f]{40}__(?P<time>[0-9]{8}-[0-9]{6})-KST__[A-Za-z0-9]{6}")
# JSON은 주석을 지원하지 않으므로 생성하는 파일에 설명을 데이터로 함께 넣는다.
METADATA_DESCRIPTIONS = {
    "started_at": "배포 시작 시각 (KST)",
    "finished_at": "배포 종료 시각 (KST)",
    "target": "이번에 배포하려는 앱의 커밋 SHA와 이미지 정보",
    "previous": "배포 전 기존 앱의 커밋 SHA, 이미지, 컨테이너와 주소",
    "phase": "마지막으로 실행한 배포 단계",
    "result": "종료 코드(0은 성공), 새 앱 전환 확인 여부, 이전 앱 정리 결과와 실패 이유",
    "rollback": "복구 결과와 시작·기존 앱 응답 복구·새 앱 정리 완료 시각 (KST)",
}
HEALTH_CHECK_DESCRIPTIONS = {
    "active_app_local": "배포 전 기존 앱에 직접 요청해 정상 응답과 커밋 SHA 확인",
    "active_app_via_domain": "배포 전 도메인을 통해 기존 앱의 정상 응답과 커밋 SHA 확인",
    "new_app_local": "새 앱에 직접 요청해 실행 준비 상태와 배포할 커밋 SHA 확인",
    "new_app_via_domain": "전환 후 도메인을 통해 새 앱의 정상 응답과 배포한 커밋 SHA 확인",
    "restored_app_via_domain": "롤백 후 도메인을 통해 기존 앱의 정상 응답과 기존 커밋 SHA 확인",
}


def kst_now():
    return datetime.now(timezone(timedelta(hours=9))).isoformat(timespec="seconds")


def write_json(path, value):
    path = Path(path)
    temporary = path.with_name("." + path.name + ".tmp")
    temporary.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n")
    temporary.replace(path)


def metadata(path, event, *args):
    path = Path(path)
    data = json.loads(path.read_text()) if path.exists() else {}
    if event == "started":
        data = {"started_at": kst_now(), "target": {"image_reference": args[0], "commit_sha": args[1]},
                "rollback": {"status": "not-needed"}}
    elif event == "previous-app":
        data["previous"] = {"commit_sha": args[0], "image_id": args[1], "color": args[2],
                            "container": args[3], "address": args[4]}
    elif event == "target-image":
        data["target"]["image_id"] = args[0]
    elif event in ("rollback-started", "rollback-recovered", "rollback-completed"):
        key = {"rollback-started": "started_at", "rollback-recovered": "traffic_restored_at",
               "rollback-completed": "completed_at"}[event]
        data.setdefault("rollback", {})[key] = kst_now()
    elif event == "finished":
        data.update(finished_at=kst_now(), phase=args[1],
                    result={"exit_code": int(args[0]), "switch_verified": args[2] == "1",
                            "cleanup": args[4], "failure_reason": args[5] or None})
        data.setdefault("rollback", {})["status"] = args[3]
    else:
        raise ValueError("알 수 없는 배포 정보 기록 단계")
    data["_descriptions"] = METADATA_DESCRIPTIONS
    write_json(path, data)


def record_health(path, name, result):
    path = Path(path)
    data = json.loads(path.read_text()) if path.exists() else {}
    if name not in HEALTH_CHECK_DESCRIPTIONS:
        raise ValueError("알 수 없는 헬스체크 기록 이름")
    data[name] = {"description": HEALTH_CHECK_DESCRIPTIONS[name], **result, "checked_at": kst_now()}
    write_json(path, data)


def command(args):
    return subprocess.check_output(args, text=True, timeout=5).strip()


def health(url, expected_sha, seconds, report_path=None, check_name=None):
    deadline = time.monotonic() + seconds
    result = {"url": url, "expected_commit_sha": None if expected_sha == "-" else expected_sha,
              "observed_commit_sha": None, "http_status": None, "status": "failed", "attempts": 0}
    while time.monotonic() < deadline:
        budget = min(3.0, deadline - time.monotonic())
        if budget <= 0:
            break
        result["attempts"] += 1
        result.update(http_status=None, observed_commit_sha=None)
        try:
            # 헤더는 표준 출력으로 받아서 임시 파일을 만들지 않는다.
            response = subprocess.run(
                ["curl", "--silent", "--show-error", "--noproxy", "*",
                 "--max-time", str(budget), "--connect-timeout", str(budget),
                 "--header", "Connection: close", "--header", "Cache-Control: no-cache",
                 "--dump-header", "-", "--output", "/dev/null", "--write-out", "\n%{http_code}",
                 url + ("&" if "?" in url else "?") + "deployment_check=" + str(time.time_ns())],
                capture_output=True, text=True, timeout=budget,
            )
            headers, _, http_status = response.stdout.rpartition("\n")
            values = re.findall(r"^Deployed-Commit-SHA:\s*([0-9a-f]{40})\s*$",
                                headers, re.IGNORECASE | re.MULTILINE)
            result.update(http_status=http_status, observed_commit_sha=values[0] if len(values) == 1 else None)
            if (time.monotonic() < deadline and response.returncode == 0
                    and http_status == "200" and len(values) == 1
                    and (expected_sha == "-" or values[0] == expected_sha)):
                result["status"] = "passed"
                if report_path:
                    record_health(report_path, check_name, result)
                print(values[0])
                return
        except (subprocess.TimeoutExpired, OSError):
            pass
        time.sleep(max(0, min(2.0, deadline - time.monotonic())))
    if report_path:
        record_health(report_path, check_name, result)
    raise RuntimeError(f"{seconds:g}초 안에 HTTP 200과 커밋 SHA를 확인하지 못했습니다: {url}")


def processes():
    # PID 재사용과 master 교체를 구분하도록 시작 시각도 함께 저장한다.
    output = command(["ps", "-C", "nginx", "-o", "pid=,ppid=,lstart=,args="])
    masters, workers, draining = [], [], []
    for line in output.splitlines():
        parts = line.split(None, 7)
        if len(parts) != 8:
            raise RuntimeError("Nginx 프로세스 정보 형식을 확인할 수 없습니다.")
        # Linux /proc의 시작 tick으로 같은 초에 PID가 재사용되는 경우도 구분한다.
        stat = Path(f"/proc/{parts[0]}/stat").read_text().rsplit(") ", 1)[1].split()
        item = {"pid": parts[0], "parent": parts[1], "started": stat[19]}
        if parts[7].startswith("nginx: master process"):
            masters.append(item)
        elif parts[7].startswith("nginx: worker process"):
            workers.append(item)
            if "shutting down" in parts[7]:
                draining.append(parts[0])
    if len(masters) != 1 or not workers or any(w["parent"] != masters[0]["pid"] for w in workers):
        raise RuntimeError("Nginx master 하나와 그 worker들을 확인할 수 없습니다.")
    return {"master": masters[0], "workers": workers, "draining": draining}


def log_position(path):
    device, inode, size = command(["sudo", "-n", "stat", "-c", "%d:%i:%s", path]).split(":")
    return {"device": device, "inode": inode, "size": int(size)}


def snapshot(path, log, previous=None):
    # 로그 위치를 먼저 저장해, 스냅샷 직후 발생한 종료 로그도 놓치지 않는다.
    position = log_position(log)
    state = processes()
    if previous is None and state["draining"]:
        raise RuntimeError("이전 reload의 worker가 아직 종료 중입니다. 새 전환을 시작하지 않습니다.")
    if previous is not None:
        original = json.loads(Path(previous).read_text())["position"]
        if (position["device"] != original["device"] or position["inode"] != original["inode"]
                or position["size"] < original["size"]):
            raise RuntimeError("기존 로그 위치를 확인할 수 없습니다.")
        # 복구 직전에도 남아 있는 Blue worker의 종료 시작 로그는 최초 전환 때 남는다.
        position = original
    state.update(log=log, position=position)
    write_json(path, state)


def worker_result(before, current, events):
    if current["master"] != before["master"]:
        raise RuntimeError("Nginx master가 바뀌었습니다. 이전 앱을 유지합니다.")
    remaining = False
    for worker in before["workers"]:
        pid = worker["pid"]
        matches = [item for item in current["workers"] if item["pid"] == pid]
        if matches and matches[0] != worker:
            raise RuntimeError("기존 worker PID가 재사용됐습니다. 종료 상태를 확정하지 않습니다.")
        if re.search(rf"worker process {pid} exited on signal", events):
            raise RuntimeError(f"worker {pid}의 비정상 종료가 기록됐습니다.")
        if matches:
            remaining = True
            continue
        graceful = re.search(rf"\[notice\] {pid}#\d+: gracefully shutting down", events)
        exited = re.search(rf"\[notice\] {before['master']['pid']}#\d+: worker process {pid} exited with code 0(?:\s|$)", events)
        if not graceful or not exited:
            remaining = True
    return not remaining


def wait_workers(path, seconds, events_path):
    before = json.loads(Path(path).read_text())
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        position = log_position(before["log"])
        original = before["position"]
        if (position["device"] != original["device"] or position["inode"] != original["inode"]
                or position["size"] < original["size"]):
            raise RuntimeError("Nginx 로그가 교체/축소돼 종료 기록을 확인할 수 없습니다. 앱을 유지합니다.")
        events = command(["sudo", "-n", "tail", "-c", f"+{original['size'] + 1}", before["log"]])
        Path(events_path).write_text(events + "\n")
        if worker_result(before, processes(), events):
            return
        time.sleep(max(0, min(1.0, deadline - time.monotonic())))
    raise RuntimeError("기존 Nginx worker의 정상 종료 확인 시간이 초과됐습니다.")


def check_timeout(path):
    text = re.sub(r"#[^\n]*", "", Path(path).read_text())
    for value in re.findall(r"\bworker_shutdown_timeout\s+([^;]+);", text):
        if value.strip() not in ("0", "0s", "0ms"):
            raise RuntimeError("worker_shutdown_timeout이 연결을 강제로 닫도록 설정돼 있습니다. 배포 스크립트의 대기 제한과 별개입니다.")


def prune_records(current_path):
    current = Path(current_path)
    root = current.parent
    if (root.is_symlink() or current.is_symlink() or not current.is_dir()
            or not RECORD_NAME.fullmatch(current.name)):
        raise RuntimeError("현재 배포 기록 폴더를 확인할 수 없어 기록 정리를 생략합니다.")
    # 이 스크립트가 만드는 이름의 실제 디렉터리만 대상으로 한다. 링크와 다른 파일은 제외한다.
    records = [path for path in root.iterdir()
               if RECORD_NAME.fullmatch(path.name) and not path.is_symlink() and path.is_dir()
               and path != current]
    records.sort(key=lambda path: (RECORD_NAME.fullmatch(path.name)["time"], path.stat().st_mtime_ns, path.name), reverse=True)
    removed, errors = 0, []
    # 현재 배포 1개 + 이전 기록 중 최신 19개. 현재 폴더는 시각과 관계없이 항상 보호한다.
    for path in records[19:]:
        try:
            shutil.rmtree(path)
            removed += 1
        except OSError as error:
            errors.append(f"{path.name}: {error}")
    print(f"배포 기록 정리: 오래된 기록 {removed}개 삭제")
    if errors:
        raise RuntimeError("일부 배포 기록을 삭제하지 못했습니다: " + "; ".join(errors))
    print("현재 배포를 포함해 최대 최근 20회분을 유지합니다.")


def cleanup_temporary(deployment_root, staging):
    root = Path(deployment_root) / "records"
    errors = []
    # 다른 도구의 임시 파일은 지우지 않고, 우리가 원자적 JSON 저장에 쓰는 경로만 정리한다.
    temporary_paths = [".deployment.json.tmp", ".health-checks.json.tmp",
                       "nginx/.workers-before-switch.json.tmp", "rollback/.nginx-workers-before-restore.json.tmp"]
    temporary_paths += [f"containers/{name}/.shutdown.json.tmp" for name in ("yeobaek-app-blue", "yeobaek-app-green")]
    if root.is_symlink():
        raise RuntimeError("배포 기록 루트가 링크여서 임시 파일 정리를 생략합니다.")
    if root.exists():
        for record in root.iterdir():
            if not RECORD_NAME.fullmatch(record.name) or record.is_symlink() or not record.is_dir():
                continue
            for relative in temporary_paths:
                path = record / relative
                if any(record.joinpath(*Path(relative).parts[:index]).is_symlink()
                       for index in range(1, len(Path(relative).parts))):
                    continue
                try:
                    path.unlink(missing_ok=True)
                except OSError as error:
                    errors.append(str(error))
    # Nginx 임시 파일은 고정 이름 하나를 사용하며, 배포 잠금을 보유한 상태에서만 정리한다.
    try:
        subprocess.run(["sudo", "-n", "rm", "-f", "--", staging], check=True, timeout=5)
    except (OSError, subprocess.SubprocessError) as error:
        errors.append(str(error))
    if errors:
        raise RuntimeError("일부 임시 파일을 정리하지 못했습니다: " + "; ".join(errors))


def main():
    action, *args = sys.argv[1:]
    if action == "health":
        health(args[0], args[1], float(args[2]), *args[3:])
    elif action == "snapshot":
        snapshot(*args)
    elif action == "wait-workers":
        wait_workers(args[0], float(args[1]), args[2])
    elif action == "check-timeout":
        check_timeout(args[0])
    elif action == "prune-records":
        prune_records(args[0])
    elif action == "metadata":
        metadata(*args)
    elif action == "record-shutdown":
        write_json(args[0], {"status": args[1], "running": args[2] == "true",
                             "exit_code": int(args[3]), "oom_killed": args[4] == "true", "checked_at": kst_now()})
    elif action == "cleanup-temporary":
        cleanup_temporary(*args)
    else:
        raise ValueError("알 수 없는 배포 보조 명령")


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, ValueError, OSError, subprocess.SubprocessError) as error:
        print(f"확인 실패: {error}", file=sys.stderr)
        sys.exit(1)
