#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ENV_FILE="$SCRIPT_DIR/.env.local"
PROJECT_SUFFIX=$(printf '%s' "$SCRIPT_DIR" | cksum | awk '{print $1}')
PROJECT_NAME=${COMPOSE_PROJECT_NAME:-yeobaek-local-$PROJECT_SUFFIX}

compose() {
  if [ -f "$ENV_FILE" ]; then
    docker compose --project-name "$PROJECT_NAME" --env-file "$ENV_FILE" "$@"
  else
    docker compose --project-name "$PROJECT_NAME" "$@"
  fi
}

assert_docker() {
  command -v docker >/dev/null 2>&1 || {
    echo "Docker를 찾을 수 없습니다. Docker Desktop 또는 Docker Engine을 설치하세요." >&2
    exit 1
  }
  docker compose version >/dev/null 2>&1 || {
    echo "Docker Compose를 사용할 수 없습니다. Docker가 실행 중인지 확인하세요." >&2
    exit 1
  }
  docker info >/dev/null 2>&1 || {
    echo "Docker 엔진에 연결할 수 없습니다. Docker Desktop 또는 Docker Engine을 시작하세요." >&2
    exit 1
  }
}

wait_backend() {
  url=http://localhost:8080/v3/api-docs
  attempt=1
  while [ "$attempt" -le 60 ]; do
    if curl --fail --silent --show-error --max-time 3 "$url" >/dev/null 2>&1; then
      echo "백엔드 준비 완료: http://localhost:8080/swagger-ui/index.html"
      return
    fi
    sleep 2
    attempt=$((attempt + 1))
  done
  compose --profile api logs --tail 100 app
  echo "백엔드가 120초 안에 준비되지 않았습니다. 위 app 로그를 확인하세요." >&2
  exit 1
}

cleanup_dev() {
  compose --profile api down --volumes --remove-orphans
}

show_help() {
  cat <<'EOF'
사용법: sh ./local-env.sh <command>
  up      사전 빌드된 백엔드 이미지와 MySQL을 실행하고 HTTP 준비를 기다림
  down    서버와 DB를 종료하고 로컬 DB 데이터도 정리
  status  컨테이너 상태 확인
  logs    서버와 DB 로그 실시간 확인
  db      IDE 실행을 위해 MySQL만 실행
  dev     MySQL을 실행하고 로컬 Gradle 서버 실행(Ctrl+C 시 MySQL 종료)
EOF
}

cd "$SCRIPT_DIR"

case "${1:-help}" in
  up)
    assert_docker
    command -v curl >/dev/null 2>&1 || {
      echo "HTTP 준비 확인에 curl이 필요합니다." >&2
      exit 1
    }
    compose --profile api pull app
    compose --profile api up -d --wait
    wait_backend
    ;;
  down)
    assert_docker
    compose --profile api down --volumes --remove-orphans
    ;;
  status)
    assert_docker
    compose --profile api ps
    ;;
  logs)
    assert_docker
    compose --profile api logs --follow app mysql
    ;;
  db)
    assert_docker
    compose --profile api stop app
    compose up -d --wait mysql
    ;;
  dev)
    assert_docker
    command -v java >/dev/null 2>&1 || {
      echo "Java를 찾을 수 없습니다. 백엔드 개발 모드는 JDK 21이 필요합니다." >&2
      exit 1
    }
    trap cleanup_dev 0
    compose --profile api stop app
    compose up -d --wait mysql
    ./gradlew bootRun --args='--spring.profiles.active=local'
    ;;
  help|-h|--help)
    show_help
    ;;
  *)
    echo "알 수 없는 명령: $1" >&2
    show_help >&2
    exit 2
    ;;
esac
