# Flyway 도입과 기존 DB baseline

기존 DB를 처음 편입하는 절차와 이후 SQL 버전 관리, 로컬·테스트 환경의 동작을 설명한다.

스키마 변경은 `src/main/resources/db/migration`의 버전 SQL로 관리한다. 앱 시작 시 Flyway가
미적용 SQL을 실행하고 Hibernate `ddl-auto=validate`가 엔티티 매핑을 검증한다.
적용한 SQL은 수정하지 않고 다음 버전 파일을 추가한다.

## 신규 DB와 기존 DB

- 빈 DB는 `V1__init_schema.sql`부터 실행한다. baseline하지 않는다.
- 기존 DB는 V1과 실제 구조가 일치하는지 확인한 뒤 **명시적으로 baseline 1을 한 번 기록**한다.
  baseline은 V1을 실행하지 않고 이력 시작점만 남긴다. 데이터나 스키마를 맞추지 않는다.
- `baseline-on-migrate=false`를 유지한다. 이력이 없는 기존 DB는 앱 기동을 실패시키는 것이 정상이다.
- V1은 제약·인덱스 이름을 통일한 운영 DB의 공통 20개 테이블을 기준으로 한다.
  별도 DDL이 적용된 DB는 추가 컬럼을 포함한
  차이를 먼저 검토하고 임의로 baseline하지 않는다.

자동 baseline은 이력이 없는 기존 DB를 V1과 비교하지 않고, 지정 버전까지 준비된 DB로 등록한다.
대상 DB를 잘못 선택했거나 필요한 DDL이 빠져 있어도 V1을 건너뛰고 후속 SQL을 실행할 수 있다.
따라서 최초 편입을 완료하지 않은 기존 DB에서는 앱 기동을 중단해, 접속 대상과 스키마를 먼저
확인하도록 한다. 기동 실패 자체가 목적이 아니라 확인하지 않은 DB에서 마이그레이션이 진행되는 것을
막기 위한 설정이다.

명시적 baseline 1은 **대상 DB가 V1과 일치함을 확인한 뒤, 이 DB를 V1까지 준비된 상태로
관리하기 시작하는 일회성 전환 단계**다. baseline 명령 자체가 구조 일치를 검증하는 것은 아니다.
편입을 마친 DB에서는 이후 배포 때 baseline을 다시 실행하지 않고 새 버전 SQL만 적용한다.

스키마 비교에서는 `flyway_schema_history`를 제외한다.
운영의 컬럼 배치는 보존하며, 환경 간 테이블 컬럼의 배치 순서만 다른 경우는 허용한다.
복합 인덱스·제약의 구성 컬럼 순서는 일치해야 한다.

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
   제약·인덱스 이름이 다르면 참조 컬럼·순서·동작을 보존하며 이름을 통일한 뒤 다시 비교한다.
5. 차이가 있으면 baseline하지 않는다. 원인을 확인하고 필요한 보정 DDL과 검증·복구 방법을 별도로 준비한다.
   Hibernate `validate`와 Flyway `validate`만으로 이 비교를 대신하지 않는다.
6. 쓰기를 중지하고 전체 백업을 만든 뒤 분리된 DB에서 복원 가능 여부를 확인한다.
   백업·복원 예시는 기존 수동 전환 문서를 따른다. baseline 전후 주요 테이블 행 수와 표본 데이터도 기록한다.

## 명시적 baseline 실행

아래 명령은 **구조를 확인한 기존 DB에 baseline 1만 기록**하는 EC2 Ubuntu Bash 명령이다.
먼저 위의 V1 비교·백업 복원 검증·쓰기 중지를 완료한다. 명령은 앱을 중지하거나 시작하지 않는다.
baseline은 실제 스키마를 검증하지 않으므로 V1 비교를 대신하지 않는다. 실행할 SQL이 없어
서버에 저장소나 migration 파일을 복사할 필요는 없다.

`dev`는 `~/yeobaek-dev`의 Compose 프로젝트 `yeobaek-dev`, `prod`는 `~/yeobaek`을 사용한다.
실행 중인 MySQL과 기존 앱 컨테이너의 설정을 읽으므로 앱 컨테이너는 중지 상태여도 된다.
앱의 DB 주소가 이 Compose의 MySQL을 가리키지 않거나 DB 이름이 다르면 실행을 거부한다.
외부 DB를 사용하는 환경은 접속 대상을 별도로 확인한 절차가 필요하다.

Flyway CLI는 앱의 Boot BOM과 같은 12.4.0을 사용한다. 공식 OSS JRE 미포함 배포판을
캐시하고 ARM64를 지원하는 Temurin 21에서 실행한다.
Boot를 올릴 때 CLI 버전도 대조한다. 서버에는 Docker Compose, `curl`, `tar`가 필요하다.
변수는 아래 서브셸이 끝나면 사라지며 `.env`나 셸 설정 파일을 바꾸지 않는다.

```bash
(
  set -euo pipefail
  umask 077

  read -r -p '대상 환경(dev/prod): ' TARGET_ENV
  case "$TARGET_ENV" in
    dev)
      cd "$HOME/yeobaek-dev"
      COMPOSE=(docker compose -p yeobaek-dev -f docker-compose.yml)
      ;;
    prod)
      cd "$HOME/yeobaek"
      COMPOSE=(docker compose -f docker-compose.yml)
      ;;
    *) printf 'dev 또는 prod를 입력해야 합니다.\n' >&2; exit 1 ;;
  esac

  mapfile -t MYSQL_CONTAINERS < <("${COMPOSE[@]}" ps --status running -q mysql)
  mapfile -t APP_CONTAINERS < <("${COMPOSE[@]}" ps -a -q app)
  if (( ${#MYSQL_CONTAINERS[@]} != 1 || ${#APP_CONTAINERS[@]} != 1 )); then
    printf '실행 중인 mysql과 기존 app 컨테이너가 각각 하나여야 합니다.\n' >&2
    exit 1
  fi
  MYSQL_CONTAINER=${MYSQL_CONTAINERS[0]}
  APP_CONTAINER=${APP_CONTAINERS[0]}
  APP_DB_URL=$(docker inspect --format '{{range .Config.Env}}{{println .}}{{end}}' \
    "$APP_CONTAINER" | sed -n 's/^DB_URL=//p')
  DB_NAME=$(docker exec "$MYSQL_CONTAINER" printenv MYSQL_DATABASE)
  FLYWAY_PASSWORD=$(docker exec "$MYSQL_CONTAINER" printenv MYSQL_ROOT_PASSWORD)
  URL_PATTERN='^jdbc:mysql://mysql:3306/([A-Za-z0-9_]+)(\?.*)?$'
  if [[ ! "$DB_NAME" =~ ^[A-Za-z0-9_]+$ ]] || [[ ! "$APP_DB_URL" =~ $URL_PATTERN ]]; then
    printf '앱 DB_URL이 Compose 내부 mysql 주소 형식이 아니거나 DB 이름이 유효하지 않습니다.\n' >&2
    exit 1
  fi
  if [[ "${BASH_REMATCH[1]}" != "$DB_NAME" || -z "$FLYWAY_PASSWORD" ]]; then
    printf '앱과 MySQL의 DB 이름이 다르거나 root 비밀번호가 없습니다.\n' >&2
    exit 1
  fi
  URL_QUERY=${BASH_REMATCH[2]}
  if [[ -n "$URL_QUERY" ]]; then
    URL_QUERY+='&allowPublicKeyRetrieval=true'
  else
    URL_QUERY='?allowPublicKeyRetrieval=true'
  fi
  FLYWAY_URL="jdbc:mysql://127.0.0.1:3306/$DB_NAME$URL_QUERY"
  FLYWAY_USER=root
  export FLYWAY_URL FLYWAY_USER FLYWAY_PASSWORD

  query() {
    docker exec "$MYSQL_CONTAINER" sh -c '
      MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot --batch --skip-column-names \
        "$MYSQL_DATABASE" -e "$1"
    ' sh "$1"
  }
  TABLE_COUNT=$(query "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'")
  HISTORY_COUNT=$(query "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'flyway_schema_history'")
  if (( TABLE_COUNT == 0 || HISTORY_COUNT != 0 )); then
    printf '빈 DB이거나 Flyway 이력이 이미 존재합니다. baseline을 실행하지 않습니다.\n' >&2
    exit 1
  fi
  printf '대상: %s / DB: %s / MySQL: %s / 앱: %s\n' \
    "$TARGET_ENV" "$DB_NAME" "$MYSQL_CONTAINER" "$APP_CONTAINER"
  read -r -p 'V1 비교·백업 복원 검증·쓰기 중지 완료 시 BASELINE 1 입력: ' CONFIRM
  [[ "$CONFIRM" == 'BASELINE 1' ]] || exit 1

  CLI_ROOT="$HOME/yeobaek-migrations/cli"
  FLYWAY_HOME="$CLI_ROOT/flyway-12.4.0"
  mkdir -p "$CLI_ROOT"
  if [[ ! -f "$FLYWAY_HOME/flyway" ]]; then
    ARCHIVE=$(mktemp --tmpdir="$CLI_ROOT" --suffix=.tar.gz flyway-12.4.0.XXXXXX)
    trap 'rm -f "$ARCHIVE"' EXIT
    curl --fail --location --output "$ARCHIVE" \
      https://github.com/flyway/flyway/releases/download/flyway-12.4.0/flyway-commandline-12.4.0.tar.gz
    tar -xzf "$ARCHIVE" -C "$CLI_ROOT"
    rm -f "$ARCHIVE"
    trap - EXIT
  fi
  for ACTION in baseline info; do
    docker run --rm --network "container:$MYSQL_CONTAINER" \
      -e FLYWAY_URL -e FLYWAY_USER -e FLYWAY_PASSWORD \
      --mount "type=bind,source=$FLYWAY_HOME,target=/flyway,readonly" \
      eclipse-temurin:21-jdk bash /flyway/flyway -baselineVersion=1 "$ACTION"
  done
  BASELINE_COUNT=$(query "SELECT COUNT(*) FROM flyway_schema_history WHERE type = 'BASELINE' AND version = '1' AND success = 1")
  [[ "$BASELINE_COUNT" == 1 ]] || { printf 'baseline 1 기록을 확인할 수 없습니다.\n' >&2; exit 1; }
  printf 'baseline 1 기록 확인 완료\n'
)
```

`baseline 1 기록 확인 완료`가 출력되면 기록 검증까지 성공한 것이다. migration 파일을 전달하지
않으므로 `No locations configured...` 또는 `No migrations found on disk.` 안내가 나올 수 있으며,
이는 스키마 검증 실패를 뜻하지 않는다.

전후 행 수·표본 데이터가 유지되는지 검사한 뒤 새 앱을 배포한다. Flyway가 후속 SQL을 실행하고
JPA 검증까지 통과하면 헬스체크와 주요 API를 확인한 뒤 쓰기를 재개한다. 이미 이력이 존재하면
baseline을 반복하지 않는다. Flyway `validate`는 migration 파일과 이력의 버전·이름·체크섬을
검사하며 실제 스키마 비교 도구가 아니다.

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
