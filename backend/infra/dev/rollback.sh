#!/usr/bin/env bash
# 사용: bash rollback.sh /절대/백업/경로
set -euo pipefail
umask 077
backup=${1:?백업 디렉터리의 절대 경로가 필요합니다}
backup=$(cd "$backup" && pwd -P)
project=$(cat "$backup/project")
deploy_dir=$(cat "$backup/deploy-dir")
cd "$deploy_dir"
# 태그가 develop의 새 이미지로 이동했어도 정확한 이전 이미지로 돌아간다.
gzip -dc "$backup/images.tar.gz" | docker image load
cp -p "$backup/docker-compose.yml" "$backup/otel-collector.yaml" "$backup/.env" "$backup/otel.env" .
if [[ -d "$backup/otel" ]]; then
  mkdir -p otel
  cp -a "$backup/otel/." ./otel/
fi
docker compose --project-directory "$deploy_dir" -p "$project" -f "$backup/compose.resolved.yml" -f "$backup/rollback-images.yml" \
  up -d --no-deps --force-recreate --pull never otel-collector
docker compose --project-directory "$deploy_dir" -p "$project" -f "$backup/compose.resolved.yml" -f "$backup/rollback-images.yml" \
  up -d --no-deps --force-recreate --pull never app
current_mysql=$(docker compose -p "$project" -f docker-compose.yml ps -q mysql)
test "$current_mysql" = "$(cat "$backup/mysql-container-id")"
docker compose -p "$project" -f docker-compose.yml ps
curl --fail --retry 20 --retry-delay 3 --retry-connrefused http://127.0.0.1:8080/health
