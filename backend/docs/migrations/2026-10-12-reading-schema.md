# 읽기 스키마 수동 전환 절차

이 절차는 `2026-10-12-reading-schema.sql`을 운영 MySQL 8.4에 한 번 적용하는 오프라인
전환 절차다. SQL은 현재 배포 스키마와 Hibernate 기준 스키마 양쪽을 검사하며,
`ck_comments_single_space`가 있는 배포 스키마에서는 작성자 이관 전에 그 제약을 제거한다.
운영 DB 실행과 애플리케이션 배포는 이 변경의 범위에 포함되지 않는다.

## 전환 전에

1. 애플리케이션을 중지해 쓰기를 차단한다.
2. DB와 컨테이너 이름을 확인하고 아래 명령을 **같은 셸에서 연속 실행**해 전체 논리 백업을 만든다.
   `set -euo pipefail`로 덤프·압축·복원 파이프라인 중 하나라도 실패하면 즉시 중단한다.

```bash
set -euo pipefail

BACKUP_DIR="$HOME/db-backup/manual"
BACKUP_FILE="$BACKUP_DIR/before_reading_schema_$(date +%Y%m%d_%H%M%S).sql.gz"
mkdir -p "$BACKUP_DIR"

docker exec yeobaek-mysql sh -c \
  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump -uroot --single-transaction --routines --triggers yeobaek' \
  | gzip > "$BACKUP_FILE"
printf 'BACKUP_FILE=%s\n' "$BACKUP_FILE"
```

3. 위에서 확정한 `BACKUP_FILE`을 다시 탐색하거나 바꾸지 말고, 파일 생성·압축·덤프 완료를 확인한다.

```bash
: "${BACKUP_FILE:?BACKUP_FILE must be set by the backup step}"
test -s "$BACKUP_FILE"
gzip -t "$BACKUP_FILE"
zcat "$BACKUP_FILE" | grep 'Dump completed' > /dev/null
BACKUP_TABLE_COUNT=$(zcat "$BACKUP_FILE" | grep -c 'CREATE TABLE')
test "$BACKUP_TABLE_COUNT" -gt 0
```

4. 마이그레이션 전에 운영 스키마와 분리된 임시 DB로 복원 검증을 반드시 수행한다. 같은 이름의
   DB가 이미 있으면 덮어쓰지 않고 중단한다. 복원과 테이블 확인이 모두 성공한 정확한 백업 경로만
   `RESTORE_VERIFIED_BACKUP_FILE`에 보존한다.

```bash
test "$(docker exec yeobaek-mysql sh -c \
  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -N -uroot -e \
   "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = '\''yeobaek_reading_schema_restore_verify'\''"')" -eq 0

docker exec yeobaek-mysql sh -c \
  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -e \
   "CREATE DATABASE yeobaek_reading_schema_restore_verify CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci"'
cleanup_restore_database() {
  docker exec yeobaek-mysql sh -c \
    'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -e \
     "DROP DATABASE IF EXISTS yeobaek_reading_schema_restore_verify"'
}
trap cleanup_restore_database EXIT

zcat "$BACKUP_FILE" | docker exec -i yeobaek-mysql sh -c \
  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot yeobaek_reading_schema_restore_verify'
RESTORED_TABLE_COUNT=$(docker exec yeobaek-mysql sh -c \
  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -N -uroot -e \
   "SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema = '\''yeobaek_reading_schema_restore_verify'\'' AND table_type = '\''BASE TABLE'\''"')
test "$RESTORED_TABLE_COUNT" -eq "$BACKUP_TABLE_COUNT"
RESTORE_VERIFIED_BACKUP_FILE="$BACKUP_FILE"

cleanup_restore_database
trap - EXIT
```

## 실행

저장소의 SQL을 서버로 복사한 뒤 애플리케이션이 중지된 상태에서 실행한다.

```bash
set -euo pipefail
docker exec -i yeobaek-mysql sh -c \
  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --show-warnings -uroot yeobaek' \
  < 2026-10-12-reading-schema.sql
```

`< 파일`로 입력하는 MySQL 배치 모드는 `--force`를 사용하지 않으면 SQL 오류에서 중단한다.
MySQL 8.4에서 지원하지 않는 `--abort-source-on-error`는 사용하지 않는다.
실행 방식은 [MySQL 배치 모드 공식 문서](https://dev.mysql.com/doc/refman/8.4/en/batch-mode.html)를 따른다.

스크립트는 다음 조건에서 첫 변경 전에 실패한다.

- 필요한 기존 테이블·컬럼·외래키가 없거나 중복된 경우
- 새 대상 테이블이 이미 존재해 재실행 또는 부분 실행이 의심되는 경우
- 댓글 공간이 없거나 두 개인 경우
- 모임 댓글 작성자를 회원으로 확정할 수 없거나 공개방 댓글 작성자가 없는 경우

대상 테이블을 `IF NOT EXISTS`로 덮어쓰거나 비우지 않는다. 이미 일부 적용된 DB에서는 전체
스크립트를 다시 실행하지 않는다.

## 검증과 기동

전환 직후 검증 SQL을 실행한다. 첫 결과 집합의 `failure_count`가 모두 `0`이고
`table_count`가 `20`인지 확인한다.

```bash
set -euo pipefail
docker exec -i yeobaek-mysql sh -c \
  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --table -uroot yeobaek' \
  < 2026-10-12-reading-schema-verify.sql
```

그 뒤 새 애플리케이션 이미지를 기동하고 `/health` 및 기존 댓글·진도 API를 점검한다.
검증 전에는 애플리케이션 쓰기를 다시 열지 않는다.

## 실패 복구

MySQL DDL은 구문마다 암묵적으로 커밋될 수 있다. 따라서 이 전환에는 전체 트랜잭션 롤백이
없다. 실행 중 하나라도 실패하면 애플리케이션을 계속 중지하고, 실패한 DB를 수동으로 이어서
수정하거나 스크립트를 재실행하지 않는다. 먼저 장애 분석용 덤프를 별도 파일로 보존한 다음,
전환 직전 전체 백업으로 DB 전체를 복원한다.

```bash
set -euo pipefail
: "${BACKUP_DIR:?BACKUP_DIR must be set by the backup step}"
: "${BACKUP_FILE:?BACKUP_FILE must be set by the backup step}"
: "${RESTORE_VERIFIED_BACKUP_FILE:?isolated restore verification must succeed before recovery}"
BACKUP_DIR_REAL=$(readlink -f "$BACKUP_DIR")
BACKUP_FILE_REAL=$(readlink -f "$BACKUP_FILE")
RESTORE_VERIFIED_BACKUP_FILE_REAL=$(readlink -f "$RESTORE_VERIFIED_BACKUP_FILE")
case "$BACKUP_FILE_REAL" in
  "$BACKUP_DIR_REAL"/before_reading_schema_*.sql.gz) ;;
  *) echo "Unexpected backup path: $BACKUP_FILE_REAL" >&2; exit 1 ;;
esac
test "$BACKUP_FILE_REAL" = "$RESTORE_VERIFIED_BACKUP_FILE_REAL"
test -s "$BACKUP_FILE_REAL"
gzip -t "$BACKUP_FILE_REAL"
zcat "$BACKUP_FILE_REAL" | grep 'Dump completed' > /dev/null

FAILED_DUMP_FILE="$BACKUP_DIR/failed_reading_schema_$(date +%Y%m%d_%H%M%S).sql.gz"
docker exec yeobaek-mysql sh -c \
  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump -uroot --routines --triggers yeobaek' \
  | gzip > "$FAILED_DUMP_FILE"
test -s "$FAILED_DUMP_FILE"
gzip -t "$FAILED_DUMP_FILE"

docker exec yeobaek-mysql sh -c \
  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -e \
   "DROP DATABASE yeobaek; CREATE DATABASE yeobaek CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci"'

zcat "$BACKUP_FILE_REAL" | docker exec -i yeobaek-mysql sh -c \
  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot yeobaek'
```

복원 뒤 기존 애플리케이션 버전으로 `/health`와 주요 데이터 건수를 확인한다. 운영 데이터베이스
삭제와 전체 복원은 되돌릴 수 없는 작업이므로 실제 장애 복구 승인 절차에 따라 수행한다.
