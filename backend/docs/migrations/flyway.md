# Flyway 도입과 기존 DB baseline

스키마 변경은 `src/main/resources/db/migration`의 버전 SQL로 관리한다. 앱 시작 시 Flyway가
미적용 SQL을 실행하고 Hibernate `ddl-auto=validate`가 엔티티 매핑을 검증한다.
적용한 SQL은 수정하지 않고 다음 버전 파일을 추가한다.

## 신규 DB와 기존 DB

- 빈 DB는 `V1__init_schema.sql`부터 실행한다. baseline하지 않는다.
- 기존 DB는 V1과 실제 구조가 일치하는지 확인한 뒤 **명시적으로 baseline 1을 한 번 기록**한다.
  baseline은 V1을 실행하지 않고 이력 시작점만 남긴다. 데이터나 스키마를 맞추지 않는다.
- `baseline-on-migrate=false`를 유지한다. 이력이 없는 기존 DB는 앱 기동을 실패시키는 것이 정상이다.
- V1은 현재 엔티티의 읽기 스키마를 기준으로 한다. 별도 DDL이 적용된 DB는 추가 컬럼을 포함한
  차이를 먼저 검토하고 임의로 baseline하지 않는다.

## 도입 전 확인

개발·운영 서버에서 각각 수행한다. 서버의 EC2 Ubuntu Bash와 기존 Docker Compose 배포 경로를 사용한다.
실제 운영 DB는 이 PR의 테스트 대상이 아니며, 테스트 통과가 운영 스키마 일치를 보증하지 않는다.

1. 배포할 커밋과 대상 DB 주소·DB 이름·MySQL 버전을 확인한다. 개발과 운영의 적용 이력을 각각 조사한다.
2. 기존 읽기 스키마 전환이 끝나지 않았다면 [기존 수동 절차](2026-10-12-reading-schema.md)를 먼저 검토한다.
3. 운영과 분리된 MySQL 8.4 임시 DB에 이번 커밋의 V1을 실행해 기준 스키마를 만든다.
   운영 DB에 V1을 직접 실행하지 않는다. 기준 DB에는 로컬 시드를 넣지 않는다.
4. 대상 DB와 기준 DB를 같은 MySQL 클라이언트의 `mysqldump --no-data --compact --no-tablespaces
   --set-gtid-purged=OFF`로 추출해 비교한다. 테이블 옵션의 `AUTO_INCREMENT=<숫자>` 현재 카운터만
   비교에서 제외할 수 있다. 컬럼의 `AUTO_INCREMENT`, 타입·길이·NULL·기본값, ENUM 값,
   charset/collation, PK·unique·인덱스·FK·ON DELETE·CHECK를 제거해 차이를 숨기지 않는다.
   기존 Hibernate의 자동 생성 제약·인덱스 이름이 다르면 참조 컬럼·순서·동작이 같은지 확인해 기록한다.
5. 차이가 있으면 baseline하지 않는다. 원인을 확인하고 필요한 보정 DDL과 검증·복구 방법을 별도로 준비한다.
   Hibernate `validate`와 Flyway `validate`만으로 이 비교를 대신하지 않는다.
6. 쓰기를 중지하고 전체 백업을 만든 뒤 분리된 DB에서 복원 가능 여부를 확인한다.
   백업·복원 예시는 기존 수동 전환 문서를 따른다. baseline 전후 주요 테이블 행 수와 표본 데이터도 기록한다.

## 명시적 baseline 실행

서버에 저장소가 없다면 **배포할 커밋의 공통 migration 디렉터리만** 전달한다.
`db/local`과 테스트 SQL을 함께 복사하지 않는다. CLI 버전은 앱의 Boot BOM이 관리하는 Flyway 12.4.0에 맞춘다.
Boot를 올릴 때 CLI 버전도 관리 의존성과 대조한다. 운영 EC2는 ARM64이며 해당 버전의 Flyway Docker
이미지는 AMD64만 제공하므로, 공식 JRE 미포함 배포판과 ARM64를 지원하는 Temurin 21 컨테이너를 사용한다.

다음 예시는 개발 서버의 `~/yeobaek-dev`, Compose 프로젝트 `yeobaek-dev`를 사용한다.
운영 서버는 `~/yeobaek`에서 `docker compose`를 사용한다. 앱이 실행 중일 때 접속 정보를 읽고,
계획한 유지보수 절차로 앱과 다른 쓰기 경로를 중지한 뒤 baseline을 수행한다. 비밀번호를 출력하지 않는다.

```bash
set -euo pipefail
CLI_ROOT="$HOME/yeobaek-migrations/cli"
mkdir -p "$CLI_ROOT"
curl --fail --location \
  --output "$CLI_ROOT/flyway-commandline-12.4.0.tar.gz" \
  https://download.red-gate.com/maven/release/com/redgate/flyway/flyway-commandline/12.4.0/flyway-commandline-12.4.0.tar.gz
tar -xzf "$CLI_ROOT/flyway-commandline-12.4.0.tar.gz" -C "$CLI_ROOT"
FLYWAY_HOME="$CLI_ROOT/flyway-12.4.0"
test -f "$FLYWAY_HOME/flyway"
docker run --rm \
  --mount "type=bind,source=$FLYWAY_HOME,target=/flyway,readonly" \
  eclipse-temurin:21-jdk bash /flyway/flyway -v
cd "$HOME/yeobaek-dev"
APP_CONTAINER=$(docker compose -p yeobaek-dev ps -q app)
test -n "$APP_CONTAINER"
FLYWAY_URL=$(docker exec "$APP_CONTAINER" printenv DB_URL)
FLYWAY_USER=$(docker exec "$APP_CONTAINER" printenv DB_USERNAME)
FLYWAY_PASSWORD=$(docker exec "$APP_CONTAINER" printenv DB_PASSWORD)
export FLYWAY_URL FLYWAY_USER FLYWAY_PASSWORD
test -n "$FLYWAY_URL" && test -n "$FLYWAY_USER" && test -n "$FLYWAY_PASSWORD"
# inspect 결과에서 DB에 접근 가능한 네트워크를 확인해 선택한다.
docker inspect --format '{{json .NetworkSettings.Networks}}' "$APP_CONTAINER"
read -r -p 'DB 접근 네트워크: ' FLYWAY_NETWORK
MIGRATION_DIR="$HOME/yeobaek-migrations/flyway"
test -f "$MIGRATION_DIR/V1__init_schema.sql"
# 위 스키마 비교·백업·쓰기 중지까지 완료한 경우에만 실행한다.
docker run --rm --network "$FLYWAY_NETWORK" \
  -e FLYWAY_URL -e FLYWAY_USER -e FLYWAY_PASSWORD \
  --mount "type=bind,source=$FLYWAY_HOME,target=/flyway,readonly" \
  --mount "type=bind,source=$MIGRATION_DIR,target=/migrations,readonly" \
  eclipse-temurin:21-jdk bash /flyway/flyway \
  -locations=filesystem:/migrations -baselineVersion=1 baseline

docker run --rm --network "$FLYWAY_NETWORK" \
  -e FLYWAY_URL -e FLYWAY_USER -e FLYWAY_PASSWORD \
  --mount "type=bind,source=$FLYWAY_HOME,target=/flyway,readonly" \
  --mount "type=bind,source=$MIGRATION_DIR,target=/migrations,readonly" \
  eclipse-temurin:21-jdk bash /flyway/flyway \
  -locations=filesystem:/migrations info
unset FLYWAY_PASSWORD FLYWAY_USER FLYWAY_URL
```

`info`와 DB의 `flyway_schema_history`에서 버전 1의 `BASELINE` 기록을 확인하고, 전후 행 수·표본이
유지되는지 검사한다. 이후 새 앱을 배포한다. Flyway가 후속 SQL을 실행하고 JPA 검증까지 통과하면
헬스체크와 주요 API를 확인한 뒤 쓰기를 재개한다. 이미 이력이 존재하면 baseline을 반복하지 않는다.
Flyway `validate`는 migration 파일과 이력의 버전·이름·체크섬을 검사하며 실제 스키마 비교 도구가 아니다.

MySQL DDL은 암묵적으로 커밋될 수 있다. 실행 실패 시 실제 적용된 구조와 이력을 먼저 조사한다.
`clean`, 이력 삭제, 무조건적인 `repair`나 baseline 버전 변경으로 실패를 숨기지 않는다.

## 로컬과 테스트

로컬은 공통 V1 뒤 `db/local/V1.1__local_seed.sql`을 한 번 적용한다. 재기동은 DB를 재생성하지 않는다.
기존 로컬 DB에 이력이 없다면 데이터 보존 여부에 맞춰 스키마·시드 상태를 확인한 후 편입하거나,
기존 DB를 유지한 채 별도의 빈 로컬 DB를 만든다. 운영 DB에 로컬 migration 경로를 사용하지 않는다.

통합 테스트도 MySQL Testcontainers에서 Flyway로 스키마를 만들고 JPA가 검증한다.
Hibernate가 대신 생성하면 누락된 DDL을 숨길 수 있으므로 `create`/`create-drop`으로 우회하지 않는다.
테스트 데이터 정리는 `flyway_schema_history`를 보존한다. 기존 수동 SQL 이력 검증 테스트만
자동 Flyway를 비활성화해 해당 SQL을 독립적으로 검증한다.

공식 참고: [baseline](https://documentation.red-gate.com/flyway/reference/commands/baseline),
[validate](https://documentation.red-gate.com/flyway/reference/commands/validate),
[Spring Boot DB 초기화](https://docs.spring.io/spring-boot/how-to/data-initialization.html).
