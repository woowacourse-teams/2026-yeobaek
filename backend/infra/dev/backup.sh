#!/usr/bin/env bash
# 개발 서버에서 실행한다. 비밀값이 포함된 백업은 본인만 읽을 수 있다.
set -euo pipefail
umask 077
cd "${DEPLOY_DIR:-$HOME/yeobaek}"
deploy_dir=$(pwd -P)
app_container=${APP_CONTAINER:-yeobaek-app}
collector_container=${COLLECTOR_CONTAINER:-yeobaek-otel-collector}
actual_dir=$(docker inspect -f '{{index .Config.Labels "com.docker.compose.project.working_dir"}}' "$app_container")
actual_dir=$(cd "$actual_dir" && pwd -P)
if [[ "$actual_dir" != "$deploy_dir" ]]; then
  echo "중단: 실행 중 앱의 Compose 경로는 $actual_dir 입니다. DEPLOY_DIR를 확인하세요." >&2
  exit 1
fi
project=$(docker inspect -f '{{index .Config.Labels "com.docker.compose.project"}}' "$app_container")
stamp=$(date +%Y%m%d-%H%M%S)
backup="$deploy_dir/backups/observability-$stamp"
mkdir -p "$backup"
cp -p docker-compose.yml otel-collector.yaml .env otel.env "$backup/"
if [[ -d otel ]]; then cp -a otel "$backup/otel"; fi
docker inspect "$app_container" > "$backup/app.inspect.json"
docker inspect "$collector_container" > "$backup/collector.inspect.json"
docker compose -p "$project" -f docker-compose.yml config > "$backup/compose.resolved.yml"
app_image=$(docker inspect -f '{{.Image}}' "$app_container")
collector_image=$(docker inspect -f '{{.Image}}' "$collector_container")
app_tag="yeobaek-backend:rollback-$stamp"
collector_tag="yeobaek-otel-collector:rollback-$stamp"
docker image inspect "$app_image" "$collector_image" > "$backup/images.inspect.json"
docker image tag "$app_image" "$app_tag"
docker image tag "$collector_image" "$collector_tag"
docker image save "$app_tag" "$collector_tag" | gzip > "$backup/images.tar.gz"
printf '%s\n' "$project" > "$backup/project"
printf '%s\n' "$deploy_dir" > "$backup/deploy-dir"
printf 'services:\n  app:\n    image: "%s"\n  otel-collector:\n    image: "%s"\n' \
  "$app_tag" "$collector_tag" > "$backup/rollback-images.yml"
# Compose 프로젝트명이 달라져 MySQL 볼륨이 바뀌는 일을 방지할 기준을 저장한다.
docker compose -p "$project" -f docker-compose.yml ps -q mysql > "$backup/mysql-container-id"
test -s "$backup/images.tar.gz"
echo "$backup"
