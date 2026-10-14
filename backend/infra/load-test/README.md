# 개발·부하테스트 DB 전환

같은 MySQL 컨테이너에 실제 개발 데이터용 DB와 더미 부하테스트용 DB를 유지한다.
백엔드 앱은 한 개만 실행하며 `use load` / `use dev`로 접속 DB를 전환한다.
안드로이드도 같은 주소를 사용하므로 전환 중에는 더미 DB를 보게 된다.

이 스크립트는 실제 책이나 회원 데이터를 복제하지 않는다. 개발 DB의 테이블 구조만 복사하고
책·저자·장·문단·문장·회원·모임·가입·진도·공개방·댓글을 새로 생성한다.
명시적인 `reset`으로 부하 DB 데이터만 비울 수 있다. 개발 DB 삭제와 자동 초기화는 제공하지 않는다.

## 서버 준비

Python 3, Docker Compose v2와 실행 중인 개발 서버가 필요하다. 저장소의 현재 읽기 스키마
(20개 테이블)가 개발 DB에 적용돼 있어야 한다. MySQL 서비스는 `MYSQL_ROOT_PASSWORD`를
제공하고 앱은 `DB_URL`로 이 MySQL 서비스에 접속해야 한다.

`load-test.py`와 `dummy_data.py`를 함께 개발 서버의 `~/yeobaek-dev/load-test/`로 복사한다.
아래 명령은 `~/yeobaek-dev/docker-compose.yml`과 같은 디렉터리의 `.env`를 사용한다.
작업 전 개발 DB 및 Compose·환경설정 파일의 백업을 확보한다.

```bash
cd ~/yeobaek-dev

# 최초 한 번: 빈 부하 DB 생성, 스키마 복사, 서버의 DB 선택 설정 설치
python3 load-test/load-test.py init

# 현재 앱이 사용하는 DB와 이후 배포에 적용될 DB 확인
python3 load-test/load-test.py status
```

기본 부하 DB 이름은 `yeobaek_load_test`다. 개발 DB 이름은 실행 중인 앱의 `DB_URL`에서
확인하며, 두 DB 이름이 같으면 중단한다. 부하 DB가 이미 존재하는 경우 덮어쓰지 않는다.
다른 이름은 최초 실행에 `init --load-database <이름>`으로 지정한다.

Compose 서비스명과 프로젝트명이 다르면 **모든 명령에 같은 옵션을 지정**한다.

```bash
python3 load-test/load-test.py \
  --compose-file docker-compose.yml --project yeobaek-dev \
  --app-service app --mysql-service mysql status
```

## 더미 생성

데이터 규모에는 기본값이 없다. 아래 수치는 실행 예시이며 원하는 값으로 변경한다.

```bash
# 예상 행 수 확인: Docker나 DB에 접속하지 않음
python3 load-test/load-test.py seed \
  --books 100 --chapters-per-book 10 --passages-per-chapter 300 \
  --sentences-per-passage 5 --members 1000 --clubs 100 \
  --members-per-club 10 --dry-run

# 같은 조건으로 부하 DB에 생성
python3 load-test/load-test.py seed \
  --books 100 --chapters-per-book 10 --passages-per-chapter 300 \
  --sentences-per-passage 5 --members 1000 --clubs 100 \
  --members-per-club 10
```

- 책당 장 10개, 장당 문단 300개, 문단당 문장 5개라면 책당 3,000문단과 15,000문장이다.
- 문단 순서는 책 전체에서 연속되며 문장 순서는 각 문단에서 시작한다.
- 책은 ID `1..books`, 회원은 `1..members`, 모임은 `1..clubs`로 생성한다.
- 모임은 책을 ID 순서로 번갈아 배정한다. 모임별 가입자는 회원 풀에서 순환 배정하며
  같은 모임 안에서 중복되지 않는다. `members-per-club`은 전체 회원 수를 초과할 수 없다.
- 책마다 공개방을 하나씩 만들고, 공개방에도 `members-per-club`명씩 방문·진도를 준비한다.
- 각 모임·공개방 회원의 진도는 그 공간의 책 중간 문단에 둔다.
- 본문 기본값은 `부하테스트용 문장입니다.`, 댓글 기본값은 `부하테스트용 댓글입니다.`이다.
  `--sentence-text`와 `--comment-text`로 텍스트·응답 크기를 맞출 수 있다.
- 책별로 10문장씩 묶어 **1·4·7번째 문장에 댓글 1개씩** 생성한다. 마지막 10문장 미만에도
  같은 위치 규칙을 적용하므로 전체 비율은 약 30%다.
- 선정된 한 문장에는 총 댓글 하나만 생성한다. 모임이 있는 책에서는 모임·공개방 댓글을
  번갈아 배치하고, 해당 책에 모임이 없으면 공개방 댓글만 만든다. 이 비율은 전체 댓글 기준이며
  각 모임이나 공개방 단독 조회에서의 밀도는 다를 수 있다.
- 저자 하나를 모든 책에 연결하고 표지는 비워 둔다. S3 객체를 생성하거나 복제하지 않는다.
- 시각은 재현 가능하도록 진도·방문 `2026-01-01 00:00:00`, 댓글 `00:00:01`로 고정한다.
- 차단·신고·댓글 조회 기록은 초기 빈 상태로 둔다. API 호출 과정에서 생성할 수 있다.
- 앱을 중지했거나 실행 앱과 배포 설정이 모두 개발 DB를 사용하는 동안 더미를 준비한다.
  부하 DB는 모든 테이블이 비어 있어야 한다.
  이미 생성된 DB에 재실행하면 중단한다. 데이터 변경은 하나의 InnoDB 트랜잭션으로 처리한다.
  생성 중 실패하면 미커밋 행은 취소하며, `COMMIT` 중 연결이 끊기면 먼저 실제 건수를 확인한다.
- `--batch-size`의 기본값은 500이다. 큰 본문 때문에 패킷 제한에 걸리면 이 값을 줄여 실행한다.

위 예시는 책 100권, 문장 1,500,000개, 댓글 450,000개를 생성한다.
규모를 크게 잡으면 트랜잭션·저장 공간이 늘어나므로 `--dry-run`의 건수를 먼저 확인한다.

## 부하 데이터를 비우고 다시 생성

API의 책 삭제는 soft-delete지만 이 명령은 MySQL의 `DELETE`로 행을 물리 삭제한다.
책뿐 아니라 부하 DB의 회원·모임·진도·댓글·차단·신고·조회 기록까지 모두 삭제한다.

```bash
cd ~/yeobaek-dev
python3 load-test/load-test.py use dev
python3 load-test/load-test.py reset

# 위 seed 명령을 원하는 규모로 다시 실행
python3 load-test/load-test.py seed \
  --books 100 --chapters-per-book 10 --passages-per-chapter 300 \
  --sentences-per-passage 5 --members 1000 --clubs 100 \
  --members-per-club 10

python3 load-test/load-test.py use load
```

`reset`과 `seed`는 앱 컨테이너가 중지(`exited`) 또는 생성 후 미실행(`created`) 상태면
선택된 DB와 관계없이 실행할 수 있다. 앱이 실행 중이면 앱과 배포 설정 모두 개발 DB를
사용해야 한다. 재시작 중이거나 설정 설치·DB 전환이 미완료이면 중단한다.
앱 컨테이너가 없으면 중지 여부를 확인할 수 없으므로 중단한다.
`reset`은 상태 파일에 저장된 부하 DB만 대상으로 삼으며,
다른 DB에서 부하 DB를 외래키로 참조하면 중단한다.
외래키 검사는 끄지 않고 자식 테이블부터 삭제한다. 전체 삭제는 하나의 트랜잭션이며
중간 오류가 나면 미커밋 삭제를 취소한다. 정상 커밋한 삭제는 복구 기능이 없으므로
유지할 부하 데이터가 있으면 실행 전에 백업한다. 테이블·인덱스·자동 증가 카운터는 유지하고
새 더미의 ID는 `seed`가 명시적으로 1부터 다시 지정한다.

이미 부하 DB를 선택한 상태라면 개발 DB 전환 대신 앱만 중지한 뒤 재생성할 수 있다.
부하 요청도 먼저 멈추고, 재생성이 끝날 때까지 앱 시작·배포를 실행하지 않는다.
아래 명령은 앞 단계가 성공한 경우에만 다음 단계를 실행한다. MySQL은 계속 실행한다.

```bash
cd ~/yeobaek-dev
docker compose -p yeobaek-dev stop app &&
python3 load-test/load-test.py reset &&
python3 load-test/load-test.py seed \
  --books 100 --chapters-per-book 10 --passages-per-chapter 300 \
  --sentences-per-passage 5 --members 1000 --clubs 100 \
  --members-per-club 10 &&
docker compose -p yeobaek-dev start app
```

`reset`과 `seed`는 DB 선택이나 컨테이너 실행 상태를 바꾸지 않는다.
`start app`은 중지 전 컨테이너의 DB로 재시작한다. 개발 DB를 선택한 상태였다면
재생성 후 `use load`로 부하 DB를 선택한다. Compose 파일·프로젝트·서비스를 다른 이름으로
사용하면 Docker 명령과 스크립트의 옵션도 동일하게 지정한다.

## 전환·측정·복귀

```bash
# 부하 DB 사용: 현재 앱 이미지를 유지하며 앱 한 개만 재생성
python3 load-test/load-test.py use load

# 부하테스트 실행 (별도 부하 도구)

# 측정 종료 후 실제 개발 DB로 복귀
python3 load-test/load-test.py use dev

python3 load-test/load-test.py status
```

두 명령 모두 `/health` 성공 및 실행 중인 앱의 `DB_URL`을 확인한 뒤 성공을 반환한다.
기본 URL은 `http://127.0.0.1:8080/health`, 대기 시간은 120초다. 포트가 다르면 전환 명령에
`--health-url <URL>`, 필요한 경우 `--health-timeout <초>`를 지정한다.
전환할 때 앱 재기동만큼 일시적으로 API를 사용할 수 없다. MySQL 컨테이너와 볼륨은 유지한다.

정상 생성한 부하 데이터는 `use dev` 후에도 보존한다. 실제 개발 데이터는 스크립트가 변경하지
않으며 앱의 기존 기동 로직과 동일하게 다뤄진다. 전환 시 현재 실행 이미지 ID를 고정하므로
서버 Compose의 오래된 이미지 태그로 교체하지 않는다. 다른 환경변수가 실행 중인 앱과
Compose 사이에서 달라졌으면 전환 전 중단하고 설정을 맞추도록 안내한다.

현재 앱 기동 시 `PublicRoomBackfill`은 해당 앱이 접속한 DB의 공개방을 보충한다.
더미에는 이미 책별 공개방이 준비돼 있어 추가 데이터가 생기지 않는다.

측정 중에는 안드로이드·관리자 요청과 `develop` 배포를 멈추고 동일한 워밍업 조건을 유지한다.
`status --counts`는 정확한 테이블 건수를 조회하므로 측정 전·후에만 실행한다.

## DB 선택 유지와 실패 확인

`init`은 서버 Compose의 `app.environment.DB_URL` 한 항목을
`${YEOBAEK_DATABASE_URL:-<원래 개발 URL>}`로 바꾼다. `use`는 같은 디렉터리 `.env`의
`YEOBAEK_DATABASE_URL` 값만 갱신한다. 현재 CI가 서버 Compose와 `.env`를 읽으므로
자동 배포 후에도 선택한 DB가 유지된다. 배포 자체는 앱 재기동과 이미지 변경을 일으키므로
부하 측정 중 배포하지 않는다. 서버 Compose를 다른 파일로 덮어쓰면 이 설정도 다시 확인한다.
`init`을 다시 실행하면 데이터를 재생성하지 않고, 현재 앱의 DB 선택을 유지하며 전환 설정을 복구한다.

서버의 `.load-test-state/`에 DB 이름·URL·MySQL 식별자·진행 상태와 최초 Compose·`.env`
백업을 보관한다. 디렉터리는 700, 상태·백업 파일은 600 권한으로 만든다. `.env` 백업에는
기존 비밀값이 포함되므로 이 디렉터리는 저장소나 공유 자료에 올리지 않는다.

전환 실패 시 자동으로 다른 DB에 접속하지 않는다. `.env`의 선택과 실제 컨테이너 상태를
`status`로 확인한 뒤 `use dev`로 복귀한다. 앱 컨테이너가 사라졌어도 전환 직전 저장한 이미지로
복귀할 수 있다. 최초 DB 생성·DDL 적용 실패 시 부분 준비된 DB를 자동 삭제하거나 덮어쓰지 않는다.
상태 파일 생성 전 실패한 초기 준비는 원인을 확인하고 별도의 새 부하 DB 이름으로 다시 준비한다.

전환 설정은 일반적인 여러 줄 형태의 Compose 환경변수 매핑·리스트를 지원한다. 인라인 YAML,
앵커로 상속한 DB_URL, 여러 호스트 JDBC URL은 자동 수정하지 않고 중단한다.

## 로컬 검증

```bash
cd backend
./gradlew bootJar
cd ..
python3 -m unittest discover -s backend/infra/load-test -p 'test_*.py' -v
```

단위 검증 외에 임시 Compose 프로젝트에서 실제 Spring 앱 이미지와 MySQL 8.4를 실행한다.
스키마만 복사되는지, 더미 비율·관계가 맞는지, 실제 API가 전환된 DB 데이터를 반환하는지,
앱 한 개·동일 이미지·동일 MySQL을 유지하는지, CI 형태로 재생성해도 DB 선택이 유지되는지,
실패한 전환에서 개발 DB로 복귀하는지, 부하 데이터만 물리 삭제하고 더 큰 조건으로 재생성하는지를
확인한다. 임시 프로젝트와 테스트 이미지는 끝나면 제거한다.
