# 부하테스트용 책 증식

기존 책을 ID 오름차순으로 번갈아 복제해 `books`의 전체 행 수를 `--total`로 맞춘다.
예를 들어 기존 3권에서 `--total 8`을 실행하면 원본 1·2·3·1·2 순서로 5권을 추가한다.
Python 3 표준 라이브러리, Docker와 MySQL 컨테이너 내부의 `mysql` 클라이언트만 사용한다.

## 개발 서버 실행

개발 서버에 이 디렉터리의 `duplicate-books.py`를 복사한다. 아래 예시는
`~/yeobaek-dev/duplicate-books.py`에 복사한 경우다. 먼저 개발 DB를 백업한다.

```bash
cd ~/yeobaek-dev
docker compose -p yeobaek-dev -f docker-compose.yml ps

# 실행 전 예상 추가 권수 확인 (DB 변경 없음)
python3 duplicate-books.py --total 1000 --dry-run

# 기존 책을 포함해 총 1,000권으로 증식
python3 duplicate-books.py --total 1000
```

Compose 서비스명이 다르면 `--service`, 프로젝트명이 다르면 `--project`, DB 이름이 다르면
`--database`를 지정한다. 컨테이너 이름으로 직접 실행할 수도 있다.

```bash
python3 duplicate-books.py --container <개발-MySQL-컨테이너명> --total 1000
```

비밀번호는 컨테이너의 `MYSQL_ROOT_PASSWORD`에서 읽고 명령줄이나 출력에 표시하지 않는다.
복제가 끝나면 JSON으로 실행 전·후 책 수와 추가 권수를 출력한다.

## 복제 범위와 실행 동작

- `books`, `author_books`, `chapters`, `passages`, `sentences`에 새 ID로 삽입한다.
- 저자는 기존 `authors` 행을 공유하고 저자와 책의 연결만 복제한다.
- 제목·출판사·출판 연도·표지 키·상태·순서·본문은 원본 그대로 복제한다. S3 객체는 복제하지 않는다.
- 회원·모임·댓글·공개방 데이터는 생성하거나 변경하지 않는다.
- 목표 수는 삭제 상태인 책도 포함한 `books`의 전체 행 수다. 원본 상태를 유지하므로
  앱에서 조회되는 활성 책 수와 다를 수 있다.
- 원본 목록은 실행 시작 시 확정한다. 같은 실행에서 추가한 책을 다시 원본으로 사용하지 않는다.
  나중에 재실행하면 그 시점의 모든 책을 원본으로 삼는다.
- 현재 권수와 목표가 같으면 아무것도 추가하지 않는다. 현재 권수보다 작은 목표는 오류로 종료하며 삭제하지 않는다.
- 복제는 하나의 InnoDB 트랜잭션으로 수행한다. 실행 중 오류가 나면 커밋되지 않은 데이터는 취소한다.
  정상 커밋한 데이터는 유지한다. 선택 삭제·정리 명령과 실행별 기록 파일은 이번 범위에서 제외한다.
- 실행 중 책 범위를 잠그므로 책 등록·수정과 서버 재기동을 함께 수행하지 않는다.
  큰 책을 많이 복제하면 트랜잭션과 DB 디스크 사용량이 커질 수 있다. 부하 측정은 증식 완료 후 시작한다.
- 연결이 `COMMIT` 중 끊기면 성공 여부가 불확실할 수 있으므로 먼저 실제 책 수를 조회한다.

현재 앱은 기동 시 `PublicRoomBackfill`로 모든 책의 공개방을 보충한다.
이번 스크립트는 책~문장만 다루므로, 공개방 API 테스트에는 공개방 준비가 별도로 필요하다.

## 검증

```bash
docker compose -p yeobaek-dev -f docker-compose.yml exec mysql \
  sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot yeobaek -e "SELECT COUNT(*) AS book_count FROM books"'
```

스크립트 자체의 통합 검증은 임시 MySQL 8.4 컨테이너를 생성하고 끝나면 제거한다.
개발 서버 DB에는 접속하지 않는다.

```bash
python3 -m unittest discover -s backend/infra/load-test -p 'test_*.py' -v
```
