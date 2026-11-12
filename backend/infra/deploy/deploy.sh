#!/usr/bin/env bash
# 사전 확인: bash deploy.sh
# EC2 배포: bash deploy.sh <Docker Hub 이미지:커밋SHA> <커밋SHA>
# deploy_support.py를 이 파일과 같은 디렉터리에 둔다.
# 준비 조건과 배포 기록은 같은 디렉터리의 README.md, 서버 설정은 ../../docs/배포_가이드.md를 참고한다.
# 기본 경로: ~/yeobaek-dev, /etc/nginx/conf.d/yeobaek-dev.conf
# 기본 도메인: https://yeobaekdev.duckdns.org/health
# Linux의 Python 3, flock, timeout과 비대화식 sudo 권한이 필요하다.
# 현재 앱도 /health에서 Deployed-Commit-SHA를 응답해야 복구 버전을 확인할 수 있다.
# 기록: ~/yeobaek-dev/deployment/records/<커밋SHA>__<KST시각>__<구분값>/
# 배포 종료 시 현재 기록을 포함해 최근 20회분만 유지한다. 정리 실패는 경고만 남긴다.
# 대기 제한: 새 앱/도메인 60초, 기존 worker 120초, Docker 종료 90초.
# WORKER_WAIT_SECONDS로 worker 대기 시간을 조정할 수 있다. 초과 시 앱을 강제 종료하지 않는다.
set -euo pipefail
umask 077

nginx_config=${NGINX_CONFIG:-/etc/nginx/conf.d/yeobaek-dev.conf}
nginx_log=${NGINX_ERROR_LOG:-/var/log/nginx/error.log}
deploy_dir=${DEPLOY_DIR:-$HOME/yeobaek-dev}
health_url=${HEALTH_URL:-https://yeobaekdev.duckdns.org/health}
worker_wait_seconds=${WORKER_WAIT_SECONDS:-120}
compose_project=${COMPOSE_PROJECT_NAME:-yeobaek-dev}
deploy_label=${DEPLOY_LABEL:-개발}
[[ "$compose_project" =~ ^[a-z0-9][a-z0-9_-]*$ ]] || {
  printf '잘못된 Compose 프로젝트 이름: %s\n' "$compose_project" >&2
  exit 1
}
nginx_staging="$(dirname "$nginx_config")/.${compose_project}-deploy.tmp"
script_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
helper="$script_dir/deploy_support.py"
record_dir=
deployment_root=
phase=preflight
routing_touched=0
switch_verified=0
new_owned=0
rollback_result=not-needed
cleanup_result=not-started
failure_reason=
warning_count=0
warning_messages=

kst_now() {
  TZ=Asia/Seoul date '+%Y-%m-%dT%H:%M:%S+09:00'
}

record_metadata() {
  python3 "$helper" metadata "$record_dir/deployment.json" "$@"
}

health_check() {
  python3 "$helper" health "$2" "$3" "$4" "$record_dir/health-checks.json" "$1"
}

log() {
  local message
  message="$(kst_now) $*"
  printf '%s\n' "$message"
  if [[ -n "$record_dir" ]]; then printf '%s\n' "$message" >> "$record_dir/deployment.log"; fi
}

warn() {
  warning_count=$((warning_count + 1))
  warning_messages+="- $*"$'\n'
  log "경고: $*"
  if [[ ${GITHUB_ACTIONS:-false} == true ]]; then printf '::warning::%s\n' "$*"; fi
}

write_actions_summary() {
  local code=$1 title switch_status="미확인"
  [[ -n ${GITHUB_STEP_SUMMARY:-} ]] || return 0
  if [[ "$code" != 0 ]]; then
    title="🔴 $deploy_label 배포 실패 — 확인 필요"
  elif [[ "$warning_count" -gt 0 ]]; then
    title="🟠 $deploy_label 배포 완료 — 경고 확인 필요"
  else
    title="🟢 $deploy_label 배포 완료"
  fi
  [[ "$switch_verified" == 1 ]] && switch_status="확인 완료"
  {
    printf '\n## %s\n\n' "$title"
    printf '| 항목 | 결과 |\n| --- | --- |\n'
    printf '| 배포 대상 커밋 | `%s` |\n' "$deploy_sha"
    printf '| 새 앱으로 전환 | %s |\n' "$switch_status"
    printf '| 마지막 단계 | `%s` |\n| 복구 결과 | `%s` |\n| 앱 정리 결과 | `%s` |\n' \
      "$phase" "$rollback_result" "$cleanup_result"
    if [[ "$code" != 0 ]]; then
      printf '\n**실패 이유:** %s\n' "${failure_reason:-명령 실행 실패 또는 실행 중단. 배포 단계 로그를 확인하세요.}"
    fi
    if [[ "$warning_count" -gt 0 ]]; then
      printf '\n### 🟠 경고 %s건\n\n%s' "$warning_count" "$warning_messages"
    fi
    printf '\n서버 기록: `%s`\n' "$record_dir"
    printf '\n무중단 여부는 외부 요청 측정 결과로 별도 확인합니다.\n'
  } >> "$GITHUB_STEP_SUMMARY"
}

fail() {
  failure_reason=$*
  printf '중단: %s\n' "$*" >&2
  exit 1
}

find_slots() {
  [[ -r "$nginx_config" ]] || fail "Nginx 설정 파일을 읽을 수 없습니다: $nginx_config"

  # 현재 사용하는 upstream 형식만 허용한다. 대상이 모호하면 추측하지 않고 중단한다.
  if ! active_address=$(awk '
    { sub(/#.*/, ""); gsub(/^[[:space:]]+|[[:space:]]+$/, "") }
    $0 == "" { next }
    /^upstream[[:space:]]+yeobaek_app[[:space:]]*\{$/ {
      blocks++; inside = 1; next
    }
    inside && $0 == "}" { inside = 0; next }
    inside {
      if ($0 ~ /^server[[:space:]]+127\.0\.0\.1:(8080|8081);$/) {
        servers++; address = $2; sub(/;$/, "", address)
      } else {
        invalid = 1
      }
    }
    END {
      if (blocks != 1 || servers != 1 || inside || invalid) exit 1
      print address
    }
  ' "$nginx_config"); then
    fail "yeobaek_app upstream에 127.0.0.1:8080 또는 8081 대상 하나가 있는지 확인하세요."
  fi

  case "$active_address" in
    127.0.0.1:8080)
      active_color=Blue; active_service=app-blue; active_container=yeobaek-app-blue
      next_color=Green; next_service=app-green; next_container=yeobaek-app-green; next_port=8081
      ;;
    127.0.0.1:8081)
      active_color=Green; active_service=app-green; active_container=yeobaek-app-green
      next_color=Blue; next_service=app-blue; next_container=yeobaek-app-blue; next_port=8080
      ;;
    *) fail "지원하지 않는 Nginx 대상입니다: $active_address" ;;
  esac
  printf 'Nginx 설정상 현재 앱: %s (%s, %s)\n' "$active_color" "$active_container" "$active_address"
}

check_current_app() {
  local details state project service bindings http_status
  # 실행 상태, Compose 프로젝트와 서비스 이름, 호스트 포트를 확인한다.
  if ! details=$(docker container inspect --format \
    '{{.State.Status}}|{{index .Config.Labels "com.docker.compose.project"}}|{{index .Config.Labels "com.docker.compose.service"}}|{{range (index .NetworkSettings.Ports "8080/tcp")}}{{.HostIp}}:{{.HostPort}};{{end}}' \
    "$active_container"); then
    fail "$active_container 상태를 확인하지 못했습니다."
  fi
  IFS='|' read -r state project service bindings <<< "$details"
  [[ "$state" == running ]] || fail "$active_container 실행 상태가 running이 아닙니다: $state"
  [[ "$project" == "$compose_project" && "$service" == "$active_service" ]] || \
    fail "$active_container 컨테이너가 예상한 Compose 서비스가 아닙니다 (프로젝트: $compose_project, 서비스: $active_service)."
  [[ "$bindings" == "$active_address;" ]] || \
    fail "$active_container 포트 연결이 예상과 다릅니다: $bindings"

  if ! http_status=$(curl --silent --show-error --noproxy '*' --connect-timeout 3 --max-time 3 \
    --output /dev/null --write-out '%{http_code}' "http://$active_address/health"); then
    fail "$active_color 앱의 /health 요청이 실패했습니다."
  fi
  [[ "$http_status" == 200 ]] || fail "$active_color 앱의 /health 응답이 200이 아닙니다: $http_status"
  printf '현재 앱 실행 상태와 로컬 /health 정상 확인\n'
}

check_next_slot() {
  local containers name
  # inspect 실패를 컨테이너 없음으로 간주하지 않는다. Docker 조회 실패도 별도로 중단한다.
  if ! containers=$(docker container ls --all --format '{{.Names}}'); then
    fail "컨테이너 목록을 확인하지 못했습니다."
  fi
  while IFS= read -r name; do
    if [[ "$name" == "$next_container" ]]; then
      fail "$next_container 컨테이너가 남아 있습니다. 정지 상태여도 자동으로 삭제하지 않습니다. 안전하게 정리할 수 있는지 먼저 확인하세요."
    fi
  done <<< "$containers"
  printf '다음 배포 위치: %s (%s, 호스트 포트 %s)\n' "$next_color" "$next_service" "$next_port"
}

compose() {
  docker compose --project-directory "$deploy_dir" --env-file "$deploy_dir/.env" \
    -p "$compose_project" -f "$deploy_dir/docker-compose.yml" -f "$record_dir/compose-image-override.yml" "$@"
}

write_override() {
  printf 'services:\n  %s:\n    image: "%s"\n' "$next_service" "$1" > "$record_dir/compose-image-override.yml"
}

collect_logs() {
  docker container logs --timestamps --tail 2000 "$1" > "$record_dir/containers/$1/application.log" 2>&1 || \
    warn "$1 로그 수집 실패. 컨테이너 상태와 함께 확인하세요."
}

stop_and_remove() {
  local container=$1 state status running exit_code oom
  log "$container 종료 신호 전송 (최대 90초 대기)"
  # Docker 명령 자체가 응답하지 않는 경우도 무한정 기다리지 않는다.
  if ! timeout 105s docker container stop --signal SIGTERM -t 90 "$container"; then
    warn "$container 종료 명령 실패. 실제 종료 상태를 확인합니다."
  fi
  if ! state=$(docker container inspect --format '{{.State.Status}}|{{.State.Running}}|{{.State.ExitCode}}|{{.State.OOMKilled}}' "$container"); then
    warn "$container 종료 상태를 확인할 수 없습니다. 삭제하지 않습니다."
    return 1
  fi
  IFS='|' read -r status running exit_code oom <<< "$state"
  if [[ "$running" != false || ( "$status" != exited && "$status" != created ) ]]; then
    warn "$container 종료를 확인하지 못했습니다: $state"
    return 1
  fi
  python3 "$helper" record-shutdown "$record_dir/containers/$container/shutdown.json" "$status" "$running" "$exit_code" "$oom" || return 1
  if [[ "$exit_code" == 137 || "$oom" == true ]]; then
    warn "$container 강제 종료 가능성 있음(exit=$exit_code, OOM=$oom). 종료 자체는 확인됐으며 무중단 여부는 외부 측정으로 판단합니다."
  elif [[ "$exit_code" == 143 ]]; then
    # 직접 보낸 SIGTERM에 해당하는 종료 코드는 OOM이 없고 종료가 확인되면 일반 기록으로 남긴다.
    log "$container SIGTERM 전송 후 종료 확인(exit=143)."
  elif [[ "$exit_code" != 0 ]]; then
    warn "$container 종료 코드=$exit_code. 종료 자체는 확인됐습니다."
  fi
  collect_logs "$container"
  # 정지된 컨테이너만 삭제한다. 이미지와 DB 볼륨은 유지한다.
  docker container rm "$container" || return 1
}

cleanup_new() {
  local containers
  collect_logs "$next_container"
  containers=$(docker container ls --all --format '{{.Names}}') || return 1
  if ! grep -Fxq "$next_container" <<< "$containers"; then return 0; fi
  stop_and_remove "$next_container"
}

install_nginx_config() {
  local source=$1 staging
  staging=$nginx_staging
  # 같은 디렉터리에서 교체해 중간까지만 작성된 설정이 보이지 않도록 한다.
  if ! sudo -n cp -p "$nginx_config" "$staging"; then return 1; fi
  if ! sudo -n tee "$staging" < "$source" > /dev/null; then
    sudo -n rm -f "$staging" || true
    return 1
  fi
  if ! sudo -n mv -f "$staging" "$nginx_config"; then
    sudo -n rm -f "$staging" || true
    return 1
  fi
}

rollback() {
  local snapshot_ok=1
  rollback_result=failed
  log "이전 앱으로 복구 시작: $active_color"
  mkdir -p "$record_dir/rollback" || return 1
  record_metadata rollback-started || return 1
  # 이때의 worker들은 Green 요청도 처리할 수 있으므로 별도 저장한다.
  python3 "$helper" snapshot "$record_dir/rollback/nginx-workers-before-restore.json" "$nginx_log" "$record_dir/nginx/workers-before-switch.json" || snapshot_ok=0
  # 배포 중 외부에서 다른 설정을 작성했다면 덮어쓰지 않고 양쪽 앱을 유지한다.
  if ! cmp -s "$nginx_config" "$record_dir/nginx/config-to-apply.conf" && \
     ! cmp -s "$nginx_config" "$record_dir/nginx/config-before-deploy.conf"; then
    warn "Nginx 설정이 배포 도중 별도로 변경됐습니다. 자동 복구를 중단합니다."
    return 1
  fi
  install_nginx_config "$record_dir/nginx/config-before-deploy.conf" || return 1
  sudo -n nginx -t || return 1
  sudo -n nginx -s reload || return 1
  if ! health_check restored_app_via_domain "$health_url" "$active_sha" 60 > /dev/null; then
    warn "이전 앱으로 복구됐는지 확인하지 못했습니다. 두 컨테이너를 유지합니다."
    return 1
  fi
  rollback_result=recovered
  record_metadata rollback-recovered || return 1
  log "이전 앱의 HTTPS 정상 응답과 SHA 확인 완료"
  if [[ "$snapshot_ok" != 1 ]] || \
     ! python3 "$helper" wait-workers "$record_dir/rollback/nginx-workers-before-restore.json" "$worker_wait_seconds" "$record_dir/rollback/nginx-worker-drain.log"; then
    cleanup_result=incomplete
    warn "복구 완료·새 앱 정리 미완료. $next_container 유지."
    return 1
  fi
  if ! stop_and_remove "$next_container"; then
    cleanup_result=incomplete
    return 1
  fi
  cleanup_result=complete
  record_metadata rollback-completed
}

finish() {
  local code=$? container retention_message
  trap - EXIT
  set +e
  if [[ "$code" != 0 ]]; then
    log "실패 단계: $phase / ${failure_reason:-명령 실행 실패 또는 실행 중단}"
    if [[ "$routing_touched" == 1 && "$switch_verified" == 0 ]]; then
      rollback || warn "복구 또는 정리가 완료되지 않았습니다. 상태 기록을 확인하세요."
    elif [[ "$routing_touched" == 0 && "$new_owned" == 1 ]]; then
      if cleanup_new; then cleanup_result=complete; else cleanup_result=incomplete; fi
    fi
    # 전환 검증 이후의 정리 실패에는 자동 롤백하지 않는다.
    if [[ ${GITHUB_ACTIONS:-false} == true ]]; then
      printf '::error::%s 배포 실패: 단계=%s, 복구=%s, 정리=%s\n' "$deploy_label" "$phase" "$rollback_result" "$cleanup_result"
    fi
  fi
  for container in yeobaek-app-blue yeobaek-app-green; do
    if ! docker container inspect --format '{{json .State}}' "$container" > "$record_dir/containers/$container/final-state.json" 2> "$record_dir/containers/$container/inspection-error.log"; then
      printf '{"inspection_status":"unavailable","reason":"컨테이너 없음 또는 상태 조회 실패"}\n' > "$record_dir/containers/$container/final-state.json"
    fi
    log "$container 상태: $(cat "$record_dir/containers/$container/final-state.json")"
  done
  record_metadata finished "$code" "$phase" "$switch_verified" "$rollback_result" "$cleanup_result" "$failure_reason" || \
    warn "배포 결과 JSON 저장 실패. deployment.log를 확인하세요."
  log "배포 종료: exit=$code, 전환확인=$switch_verified, 복구=$rollback_result, 정리=$cleanup_result"
  log "기록 위치: $record_dir"
  if ! python3 "$helper" cleanup-temporary "$deployment_root" "$nginx_staging"; then
    warn "임시 파일 정리 실패. 다음 배포에서 다시 정리합니다."
  fi
  # 성공·실패 결과를 모두 기록한 뒤 정리한다. 기록 삭제 실패로 배포 결과를 바꾸지 않는다.
  if retention_message=$(python3 "$helper" prune-records "$record_dir" 2>&1); then
    log "$retention_message"
  else
    warn "오래된 배포 기록 정리 실패. 배포 결과는 유지합니다. $retention_message"
  fi
  # 기록 정리 과정에서 발생한 경고까지 포함한다. 요약 저장 실패로 배포 결과를 바꾸지 않는다.
  write_actions_summary "$code" || warn "Actions 요약 저장 실패. 배포 단계 로그를 확인하세요."
  exit "$code"
}

[[ $# -eq 0 || $# -eq 2 ]] || fail "사용: bash deploy.sh [이미지:커밋SHA 커밋SHA]"
for command_name in awk docker curl; do
  command -v "$command_name" >/dev/null 2>&1 || fail "필요한 명령이 없습니다: $command_name"
done

# 인자가 없으면 기존과 같이 읽기 전용 사전 확인만 한다.
if [[ $# -eq 0 ]]; then
  find_slots
  check_current_app
  check_next_slot
  printf '사전 확인 완료. 실제 Nginx 경유 응답 확인과 배포 작업은 아직 수행하지 않았습니다.\n'
  exit 0
fi

deploy_image=$1
deploy_sha=$2
[[ "$deploy_sha" =~ ^[0-9a-f]{40}$ ]] || fail "40자리 커밋 SHA가 필요합니다."
[[ "$deploy_image" =~ ^[a-z0-9][a-z0-9./:_-]*$ && "$deploy_image" == *":$deploy_sha" ]] || \
  fail "이미지 태그는 전달한 커밋 SHA와 같아야 합니다."
[[ "$health_url" == https://* ]] || fail "도메인 확인 URL은 HTTPS여야 합니다."
[[ "$worker_wait_seconds" =~ ^[1-9][0-9]*$ ]] || fail "WORKER_WAIT_SECONDS는 양의 정수여야 합니다."
for command_name in python3 sudo flock timeout ps stat cmp; do
  command -v "$command_name" >/dev/null 2>&1 || fail "필요한 명령이 없습니다: $command_name"
done
[[ -f "$helper" ]] || fail "deploy_support.py가 스크립트 옆에 필요합니다."
[[ -d "$deploy_dir" ]] || fail "배포 디렉터리가 없습니다: $deploy_dir"
deploy_dir=$(cd "$deploy_dir" && pwd -P)
deployment_root="$deploy_dir/deployment"
[[ ! -L "$deployment_root" && ! -L "$deployment_root/records" ]] || fail "배포 기록 경로는 심볼릭 링크가 아니어야 합니다."
mkdir -p "$deployment_root/records"
exec 9> "$deployment_root/deploy.lock"
flock -n 9 || fail "다른 배포가 실행 중입니다."
# 중단된 이전 실행이 남긴 임시 파일을 배포 잠금을 잡은 뒤 정리한다.
if ! python3 "$helper" cleanup-temporary "$deployment_root" "$nginx_staging"; then
  warn "이전 배포의 임시 파일 정리 실패. 배포 종료 때 다시 정리합니다."
fi
record_dir=$(mktemp -d "$deployment_root/records/${deploy_sha}__$(TZ=Asia/Seoul date +%Y%m%d-%H%M%S)-KST__XXXXXX")
mkdir -p "$record_dir/nginx" "$record_dir/containers/yeobaek-app-blue" "$record_dir/containers/yeobaek-app-green"
trap finish EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
log "$deploy_label 배포 시작: $deploy_image"
record_metadata started "$deploy_image" "$deploy_sha"

find_slots
check_current_app
check_next_slot
[[ ! -L "$nginx_config" ]] || fail "Nginx 설정은 심볼릭 링크가 아닌 파일이어야 합니다."
[[ -f "$deploy_dir/docker-compose.yml" && -f "$deploy_dir/.env" ]] || fail "서버 Compose와 .env가 필요합니다."
active_sha=$(health_check active_app_local "http://$active_address/health" - 3) || \
  fail "현재 앱의 커밋 SHA를 확인할 수 없습니다. SHA 응답 기능이 있는 앱을 먼저 준비하세요."
health_check active_app_via_domain "$health_url" "$active_sha" 3 > /dev/null || \
  fail "현재 앱의 SHA와 도메인 응답이 일치하지 않습니다."
previous_image_id=$(docker container inspect --format '{{.Image}}' "$active_container")
record_metadata previous-app "$active_sha" "$previous_image_id" "$active_color" "$active_container" "$active_address"
sudo -n nginx -T > "$record_dir/nginx/all-configs-before-deploy.conf" 2> "$record_dir/nginx/config-check.log" || fail "현재 Nginx 설정 검사 실패"
grep -Fxq "# configuration file $nginx_config:" "$record_dir/nginx/all-configs-before-deploy.conf" || \
  fail "변경 대상 설정 파일이 Nginx에 포함돼 있지 않습니다."
python3 "$helper" check-timeout "$record_dir/nginx/all-configs-before-deploy.conf"
cp "$nginx_config" "$record_dir/nginx/config-before-deploy.conf"

phase=start-new-app
write_override "$deploy_image"
compose config --quiet "$next_service"
compose pull "$next_service"
# pull한 이미지 ID로 고정해 실행 도중 태그가 바뀌어도 같은 이미지를 사용한다.
image_id=$(docker image inspect --format '{{.Id}}' "$deploy_image")
[[ "$image_id" =~ ^sha256:[0-9a-f]{64}$ ]] || fail "pull한 이미지 ID를 확인하지 못했습니다."
write_override "$image_id"
record_metadata target-image "$image_id"
check_next_slot
new_owned=1
compose up -d --no-deps --pull never "$next_service"
phase=local-health
health_check new_app_local "http://127.0.0.1:$next_port/health" "$deploy_sha" 60 > /dev/null || \
  fail "새 앱이 60초 안에 준비되지 않았습니다. 기존 앱을 유지합니다."

phase=switch-nginx
cmp -s "$nginx_config" "$record_dir/nginx/config-before-deploy.conf" || fail "배포 중 Nginx 설정이 변경됐습니다."
# upstream 안의 서버 주소만 바꾼다. 다른 location, 인증서, 제한 설정은 보존한다.
awk -v old="$active_address" -v replacement="127.0.0.1:$next_port" '
  /^[[:space:]]*upstream[[:space:]]+yeobaek_app[[:space:]]*\{/ { inside=1 }
  inside && /^[[:space:]]*server[[:space:]]/ { sub(old, replacement) }
  { print }
  inside && /^[[:space:]]*}/ { inside=0 }
' "$record_dir/nginx/config-before-deploy.conf" > "$record_dir/nginx/config-to-apply.conf"
python3 "$helper" snapshot "$record_dir/nginx/workers-before-switch.json" "$nginx_log"
routing_touched=1
install_nginx_config "$record_dir/nginx/config-to-apply.conf" || fail "Nginx 설정 교체 실패"
sudo -n nginx -t || fail "변경한 Nginx 설정 검사 실패"
sudo -n nginx -s reload || fail "Nginx reload 실패"

phase=public-health
health_check new_app_via_domain "$health_url" "$deploy_sha" 60 > /dev/null || \
  fail "도메인에서 새 앱의 정상 응답과 SHA를 확인하지 못했습니다."
switch_verified=1
log "전환 완료: $next_color, SHA=$deploy_sha"

phase=drain-old-workers
cleanup_result=incomplete
python3 "$helper" wait-workers "$record_dir/nginx/workers-before-switch.json" "$worker_wait_seconds" "$record_dir/nginx/worker-drain.log" || \
  fail "전환 완료·이전 앱 정리 미완료. $active_container 유지."
phase=stop-old-app
stop_and_remove "$active_container" || fail "전환 완료·이전 앱 종료/삭제 확인 실패."
cleanup_result=complete
phase=complete
log "새 앱 전환과 이전 앱 정리 완료"
