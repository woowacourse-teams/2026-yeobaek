# Flyway로 책 상세 정보 추가

- V4: `description TEXT NULL`, `body_character_count BIGINT NULL` 컬럼을 추가한다.
- V5: 기존 책 중 `body_character_count IS NULL`인 책의 본문 글자 수를 계산해 저장한다.

두 버전 모두 앱에 포함되어 시작할 때 자동 실행된다. 별도 backfill 실행이나 수동 ALTER는 필요 없다.
소개와 본문은 변경하지 않고 이미 저장된 글자 수도 보존한다. 미집계 값은 `NULL`이다.

## 개발·운영 EC2 Ubuntu 실행 순서

1. 배포 가이드의 [서버 선택](../배포_가이드.md#서버-선택)에 따라 환경과 DB 백업을 확인한다. 기존 DB에 Flyway 이력이 없다면 [최초 baseline 절차](../배포_가이드.md#기존-db의-최초-baseline-및-실패-복구-도구)에 따라 V1 스키마 확인 후 baseline 1을 먼저 기록한다.
2. 기존 앱과 업로드를 중단한다. 배포 자동화가 동시에 앱을 시작하지 않도록 작업 시간을 조율한다.
3. V4·V5이 포함된 새 이미지를 배포한다. Flyway 완료 후 Hibernate 검증과 앱 기동이 이어진다.
4. 이력과 미집계 수를 확인하고 트래픽을 재개한다.

아래 명령은 배포 가이드에서 `COMPOSE` 배열을 선택한 같은 셸에서 실행한다.
배포할 커밋 이미지 주소를 `RELEASE_IMAGE`로 설정한다. 개발 서버의 가변 `develop` 태그 대신
검증한 커밋 태그를 사용하고, 운영도 배포 대상 커밋과 일치시킨다.

```bash
: "${RELEASE_IMAGE:?배포할 커밋 이미지 주소를 설정하세요}"
OVERRIDE=$(mktemp ./book-detail-image.XXXXXX)
trap 'rm -f "$OVERRIDE"; unset FLYWAY_PASSWORD' EXIT
printf 'services:\n  app:\n    image: "%s"\n' "$RELEASE_IMAGE" > "$OVERRIDE"
"${COMPOSE[@]}" -f "$OVERRIDE" pull app
"${COMPOSE[@]}" stop app
"${COMPOSE[@]}" -f "$OVERRIDE" up -d app
"${COMPOSE[@]}" logs --tail 100 app
```

앱 시작에 실패하면 트래픽을 열지 않고 아래 복구 절차를 따른다.
마이그레이션이 오래 걸릴 수 있으므로 로그와 DB 상태로 진행을 확인하고 중복 실행하지 않는다.
DB 컨테이너와 볼륨은 유지한다.

서버에 이 문서와 같은 버전의 `2026-10-19-book-detail-verify.sql`을
`$HOME/yeobaek-migrations/`에 준비하고 실행한다.

```bash
MYSQL_CONTAINER=$("${COMPOSE[@]}" ps -q mysql)
test -n "$MYSQL_CONTAINER"
docker exec -i "$MYSQL_CONTAINER" sh -c \
  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --table -uroot yeobaek' \
  < "$HOME/yeobaek-migrations/2026-10-19-book-detail-verify.sql"
```

V4·V5 성공 이력, 활성 책 미집계 0건, 비양수 0건을 확인한다.
소개는 선택 정보이므로 `NULL`이어도 정상이다.

## 계산 기준과 실패 복구

책 ID 순서로 처리하며 한 문단의 문장들을 연결해 NFC 정규화한 뒤 CR/LF/탭을 제외한
유니코드 문자 수를 합한다. 공백·문장부호는 포함한다. 문단 사이에서는 정규화를 합치지 않는다.
쪽수는 저장하지 않고 앱에서 700자로 나눠 올림한다.
역사 마이그레이션의 계산은 앱 VO에 의존하지 않으며, 이후 정책 변경은 새 버전으로 작성한다.

JDBC 스트리밍으로 한 문단만 버퍼링한다. 챕터·문단·문장이 없거나 본문이 유효하지 않으면
책 ID와 문단 ID가 포함된 오류로 즉시 중단한다. 기본 문단 버퍼 한도는 UTF-16 단위 1,048,576이다.
한도가 원인이라면 메모리를 확인하고 Flyway placeholder `bookbodyparagraphbufferlimit`를
양의 정수로 높여 재실행한다. 저장할 글자 수의 계산 기준은 바뀌지 않는다.

V5의 InnoDB 데이터 변경은 Flyway 트랜잭션으로 실행한다. 중간 실패하면 그 실행의 글자 수 변경은
롤백되지만, MySQL의 V4 DDL과 실패 이력까지 되돌아가지는 않는다.
실패 원인과 실제 값을 확인한 뒤 [배포 가이드의 도구](../배포_가이드.md#기존-db의-최초-baseline-및-실패-복구-도구)로
**같은 배포 JAR와 classpath**에서 `flyway_admin repair`를 실행하고 새 앱을 다시 시작한다.
SQL 파일만 연결한 CLI로 repair하지 않는다. 정상 적용 이력을 삭제하거나 성공으로 위조하지 않는다.

버퍼 상향 예시는 다음과 같다. repair 후 앱에도 동일한 설정을 전달해야 한다.

```bash
# 배포 가이드의 flyway_admin 함수를 준비한 뒤:
flyway_admin -placeholders.bookbodyparagraphbufferlimit=2097152 repair
# 앱 Compose의 environment에 아래 값을 추가한 override와 이미지 override를 함께 사용해 다시 생성한다.
# SPRING_FLYWAY_PLACEHOLDERS_BOOKBODYPARAGRAPHBUFFERLIMIT: "2097152"
```

V5가 성공한 이후에는 앱을 재시작해도 다시 보충하지 않는다.
이전 앱으로 롤백한 동안 새 NULL 데이터가 생겼다면 원인을 확인하고 후속 버전 마이그레이션으로 처리한다.
컬럼과 계산 결과를 지우거나 이미 적용한 V5 소스를 고쳐 재실행하지 않는다.
