# 여백 DB 백업 운영 가이드

EC2의 Docker MySQL에 있는 `yeobaek` DB를 매일 자동으로 백업하고 7일치를 보관합니다. 이 문서는 왜 이렇게 구성했는지, 백업이 정상인지 확인하는 법, 문제가 생겼을 때 복원하는 법을 정리합니다.

| 항목 | 내용 |
|---|---|
| 작성일 | 2026-09-16 |
| 서버 | EC2 Ubuntu (시간대 Asia/Seoul) |
| 대상 DB | yeobaek |

## 목차

1. [배경과 목적](#1-배경과-목적)
2. [백업 정책](#2-백업-정책)
3. [동작 구조](#3-동작-구조)
4. [스크립트와 cron](#4-스크립트와-cron)
5. [일상 점검](#5-일상-점검)
6. [복원 테스트](#6-복원-테스트)
7. [장애 시 복원](#7-장애-시-복원)
8. [문제 해결](#8-문제-해결)
9. [한계와 다음 단계](#9-한계와-다음-단계)

---

## 1. 배경과 목적

여백의 모든 데이터는 EC2 한 대의 Docker 컨테이너 안 MySQL에 있습니다. 잘못된 쿼리, 마이그레이션 실수, 컨테이너나 볼륨 손상이 생기면 데이터를 되돌릴 방법이 없었습니다. 그래서 하루 단위로 되돌아갈 수 있는 지점을 만들어 두는 것이 이 백업의 목적입니다.

### 백업 파일의 실체

백업 파일(`.sql.gz`)은 DB를 처음부터 다시 만드는 SQL 명령어를 텍스트로 적고 gzip으로 압축한 파일입니다. 안에는 테이블마다 아래 같은 내용이 들어 있습니다.

```sql
DROP TABLE IF EXISTS `members`;
CREATE TABLE `members` ( ... );
INSERT INTO `members` VALUES (1, ...), (2, ...);
```

복원할 때는 이 SQL을 MySQL에 그대로 실행합니다. 파일 하나만 있으면 다른 서버에서도 같은 DB를 만들 수 있습니다.

## 2. 백업 정책

| 항목 | 정책 |
|---|---|
| 대상 | `yeobaek` DB만 백업합니다. `mysql`, `sys`, `information_schema`, `performance_schema`는 시스템 스키마라서 제외합니다. |
| 방식 | 논리 백업 (`mysqldump --single-transaction`). 테이블 락 없이 한 시점 기준으로 뜨기 때문에 서비스 중에도 실행할 수 있습니다. |
| 주기 | 매일 00:00 (KST) |
| 보관 | 최근 7일. 그보다 오래된 파일은 백업이 성공한 경우에만 자동으로 삭제합니다. |
| 위치 | `/home/ubuntu/db-backup/files/` |
| 파일명 | `yeobaek_YYYYMMDD_HHMM.sql.gz` |
| 크기 | 1개 약 15MB, 7일치 약 105MB (2026-09-16 기준, DB 원본 데이터 약 52MB) |
| 복구 가능 범위 | 최대 하루 전 상태로 되돌릴 수 있습니다. 마지막 백업 이후 들어온 데이터는 복구되지 않습니다. |

## 3. 동작 구조

```text
cron (매일 00:00)
  → 덤프 + 압축   : 컨테이너 안 mysqldump 결과를 gzip으로 저장
  → 검증          : 마지막 줄에 "Dump completed"가 없으면 실패 처리
  → 정리          : 성공한 경우에만 7일 초과 파일 삭제
```

실행 결과는 모두 `/home/ubuntu/db-backup/backup.log`에 쌓입니다.

```text
/home/ubuntu/db-backup/
├── backup.sh      # 백업 스크립트
├── backup.log     # 실행 로그
└── files/         # 백업 파일 (최근 7개)
    ├── yeobaek_20260916_0000.sql.gz
    └── ...
```

## 4. 스크립트와 cron

### backup.sh

```bash
#!/bin/bash
set -euo pipefail

CONTAINER="yeobaek-mysql"
DB="yeobaek"
BACKUP_DIR="$HOME/db-backup/files"
KEEP_DAYS=7
NOW=$(date +%Y%m%d_%H%M)
FILE="$BACKUP_DIR/${DB}_${NOW}.sql.gz"

mkdir -p "$BACKUP_DIR"

# 1) 덤프 + 압축 (비밀번호는 컨테이너 환경변수에서 읽음)
docker exec "$CONTAINER" sh -c \
  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump -uroot --single-transaction --routines --triggers '"$DB" \
  | gzip > "$FILE"

# 2) 검증: 덤프가 끝까지 됐는지 확인
if ! zcat "$FILE" | tail -n 1 | grep -q "Dump completed"; then
  echo "[$(date)] 백업 실패: $FILE" >&2
  rm -f "$FILE"
  exit 1
fi

# 3) 7일 초과분 삭제 (백업 성공 시에만 도달)
find "$BACKUP_DIR" -name "${DB}_*.sql.gz" -mtime +$((KEEP_DAYS - 1)) -delete

echo "[$(date)] 백업 완료: $FILE ($(du -h "$FILE" | cut -f1))"
```

- `set -o pipefail`: mysqldump가 실패하면 스크립트 전체가 실패로 끝납니다. 이게 없으면 빈 파일이 성공한 것처럼 남습니다.
- 비밀번호는 컨테이너의 `MYSQL_ROOT_PASSWORD`를 쓰기 때문에 스크립트나 셸 히스토리에 남지 않습니다.
- 삭제를 맨 마지막에 두어서, 백업이 실패한 날에는 기존 백업이 지워지지 않습니다.

### cron 등록

`crontab -e`로 열어 아래 한 줄을 등록했습니다. `crontab -l`로 확인할 수 있습니다.

```cron
0 0 * * * /home/ubuntu/db-backup/backup.sh >> /home/ubuntu/db-backup/backup.log 2>&1
```

| 필드 | 값 | 의미 |
|---|---|---|
| 분 · 시 | `0 0` | 00시 00분 |
| 일 · 월 · 요일 | `* * *` | 매일 |
| 리다이렉트 | `>> ... 2>&1` | 출력과 에러를 로그 파일에 이어서 기록 |

> [!WARNING]
> 시간대 주의: EC2 기본 시간대는 UTC입니다. 이 서버는 `sudo timedatectl set-timezone Asia/Seoul`로 KST로 바꿔 두었습니다. 서버를 새로 만들면 이 설정도 다시 해야 하며, 그렇지 않으면 백업이 오전 9시에 실행됩니다.

## 5. 일상 점검

주 1회 정도 아래 명령으로 확인합니다.

```bash
# 최근 실행 결과: "백업 완료"가 매일 찍혀 있는지
tail -n 20 ~/db-backup/backup.log

# 파일이 7개 안팎이고 크기가 비슷한지
ls -lh ~/db-backup/files

# 가장 최근 파일이 손상되지 않았는지
gzip -t "$(ls -t ~/db-backup/files/*.sql.gz | head -1)" && echo OK
```

- [ ] 로그에 날짜별로 "백업 완료"가 빠짐없이 있다
- [ ] 파일 크기가 갑자기 줄지 않았다 (크게 줄었다면 데이터 유실이나 덤프 오류를 의심)
- [ ] `gzip -t`가 OK를 출력한다
- [ ] 디스크 여유가 충분하다 (`df -h`)

파일 안에 테이블이 모두 있는지 보려면 아래처럼 확인합니다.

```bash
zcat <백업파일> | grep "CREATE TABLE"
```

## 6. 복원 테스트

파일이 멀쩡한 것과 실제로 복원되는 것은 다릅니다. 운영 DB는 건드리지 않고 임시 컨테이너에 복원해서 확인합니다. 스키마를 크게 바꾼 뒤나 한 달에 한 번 정도 실행하는 것을 권장합니다.

```bash
# 1) 임시 MySQL 띄우기 (운영과 같은 버전, 포트는 열지 않음)
docker run -d --name restore-test \
  -e MYSQL_ROOT_PASSWORD=test -e MYSQL_DATABASE=yeobaek mysql:8.4
sleep 30

# 2) 백업 복원
zcat <백업파일> | docker exec -i restore-test sh -c 'MYSQL_PWD=test mysql -uroot yeobaek'

# 3) 결과 확인
docker exec restore-test sh -c 'MYSQL_PWD=test mysql -uroot yeobaek -e "SHOW TABLES"'
docker exec restore-test sh -c 'MYSQL_PWD=test mysql -uroot yeobaek -e "SELECT COUNT(*) FROM <주요테이블>"'

# 4) 정리
docker rm -f restore-test
```

운영 DB에서도 같은 테이블의 `COUNT(*)`를 조회해 비교합니다. 백업 이후 새로 들어온 만큼의 차이는 정상입니다.

> [!NOTE]
> 메모리가 작은 인스턴스라면 MySQL을 하나 더 띄우기 버거울 수 있습니다. 그럴 땐 `scp`로 파일을 로컬에 받아 로컬 Docker에서 같은 절차로 확인합니다.

## 7. 장애 시 복원

> [!CAUTION]
> 운영 DB를 덮어쓰는 작업입니다. 백업 파일에는 `DROP TABLE`이 들어 있어서, 복원하면 현재 테이블이 백업 시점 상태로 교체됩니다. 백업 이후 데이터는 사라지니 팀에 먼저 공유하고 진행합니다.

1. 팀에 복원 사실과 되돌릴 시점을 공유합니다.
2. 데이터가 더 바뀌지 않도록 애플리케이션을 멈춥니다.
3. 현재 상태도 혹시 모르니 따로 떠 둡니다.

   ```bash
   docker exec yeobaek-mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump -uroot --single-transaction yeobaek' \
     | gzip > ~/db-backup/before_restore_$(date +%Y%m%d_%H%M).sql.gz
   ```

4. 되돌릴 백업 파일을 골라 복원합니다.

   ```bash
   zcat ~/db-backup/files/yeobaek_YYYYMMDD_HHMM.sql.gz \
     | docker exec -i yeobaek-mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot yeobaek'
   ```

5. 테이블과 주요 데이터 건수를 확인한 뒤 애플리케이션을 다시 띄웁니다.
6. 무슨 일이 있었고 어느 시점으로 되돌렸는지 기록합니다.

`before_restore_` 파일은 `files/` 밖에 두었기 때문에 자동 삭제 대상이 아닙니다. 상황이 정리되면 직접 지웁니다.

## 8. 문제 해결

| 증상 | 원인과 조치 |
|---|---|
| 로그 파일이 생기지 않음 | cron이 동작하지 않는 상태입니다. `systemctl status cron`으로 확인하고, 경로가 절대 경로인지 봅니다. |
| `permission denied` | 실행 권한이 없습니다. `chmod +x ~/db-backup/backup.sh` |
| `docker: command not found` | cron은 PATH가 짧습니다. `which docker` 결과(예: `/usr/bin/docker`)를 스크립트에 전체 경로로 적습니다. |
| `permission denied ... docker.sock` | 사용자가 docker 그룹에 없습니다. `sudo usermod -aG docker $USER` 후 다시 로그인합니다. |
| 실행 시각이 9시간 어긋남 | 서버 시간대가 UTC입니다. `timedatectl`로 확인 후 Asia/Seoul로 바꾸고 cron을 재시작합니다. |
| "백업 실패" 로그 | 컨테이너가 꺼져 있거나 이름이 바뀌었을 가능성이 큽니다. `docker ps`로 확인하고 스크립트의 `CONTAINER`를 맞춥니다. |
| 파일 크기가 급감 | 테이블이 비었거나 덤프가 일부만 됐을 수 있습니다. `grep "CREATE TABLE"`로 테이블 목록을 확인하고, 운영 DB 건수와 비교합니다. |

## 9. 한계와 다음 단계

- 백업이 운영 DB와 같은 EC2 디스크에 있습니다. 인스턴스나 볼륨이 사라지면 백업도 함께 사라집니다. S3 같은 외부 저장소로 복사하는 것이 다음 우선순위입니다.
- 실패해도 로그에만 남고 알림이 가지 않습니다. 실패 시 Slack·Discord 웹훅으로 알리는 것을 검토합니다.
- 하루 단위라 마지막 백업 이후 데이터는 복구할 수 없습니다. 더 짧은 복구 시점이 필요해지면 binlog 기반 시점 복구를 검토합니다.
- 데이터가 늘면 파일 크기와 백업 시간도 늘어납니다. 몇 달에 한 번 크기를 다시 점검합니다.

---

## 변경 이력

| 날짜 | 내용 |
|---|---|
| 2026-09-16 | 최초 작성 (매일 00:00 백업, 7일 보관 적용) |
