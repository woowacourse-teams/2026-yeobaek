# 여백 백엔드

온라인 교환독서 플랫폼 "여백"의 백엔드 서버입니다. (Java 21 / Spring Boot / MySQL)

> 팀에 새로 합류한 개발자는 [온보딩 문서](docs/온보딩_프로젝트_개발_방법.md)부터 읽어야 합니다.

## 디렉터리 구성

| 경로 | 용도 |
|---|---|
| `src/main/`, `src/test/` | 애플리케이션 코드·리소스와 테스트 |
| `infra/local/` | 로컬 Compose 설정 |
| `infra/prod/` | 운영 이미지, Compose, S3 인프라 설정 |
| `config/` | PMD·SpotBugs 정적 분석 설정 |
| `gradle/` | Gradle Wrapper와 동시 실행 잠금 도구·검증 |
| `docs/` | 온보딩·배포 문서, ADR, 개발 지침과 템플릿 |

루트에는 Gradle 빌드 파일과 Wrapper 진입점, README·에이전트 지침, Git 설정을 둡니다.
`build/`, `.gradle/`, IDE 설정은 Git에서 제외되는 생성물입니다.
로컬 Compose 설정은 [`infra/local/docker-compose.local.yml`](infra/local/docker-compose.local.yml),
운영 설정은 `infra/prod/`에 있습니다. 아래 명령은 `backend/`에서 실행합니다.

Docker 이미지는 `backend/`를 빌드 컨텍스트로 유지합니다.

```bash
./gradlew bootJar
docker build -f infra/prod/Dockerfile -t yeobaek-backend .
```

## 로컬 실행

Android/API 연동용 서버와 MySQL은 Docker로 실행합니다.

```bash
docker compose --project-name yeobaek-local -f infra/local/docker-compose.local.yml --profile api up -d
```

백엔드 코드를 개발할 때는 JDK 21과 Docker를 준비하고 DB만 실행한 뒤 Wrapper로 서버를 시작합니다.
이미 API 컨테이너가 실행 중이면 먼저 중지해 호스트 서버와 포트가 충돌하지 않게 합니다.

```bash
docker compose --project-name yeobaek-local -f infra/local/docker-compose.local.yml stop app
docker compose --project-name yeobaek-local -f infra/local/docker-compose.local.yml up -d --wait mysql
./gradlew bootRun --args='--spring.profiles.active=local'
```

작업 후 로컬 컨테이너를 종료합니다.

```bash
docker compose --project-name yeobaek-local -f infra/local/docker-compose.local.yml down
```

다른 clone·worktree를 동시에 실행하면 각각 다른 `--project-name`과 호스트 포트를 사용합니다.
Windows에서는 `./gradlew`를 `.\gradlew.bat`으로 바꿉니다.

## 빌드와 테스트

```bash
./gradlew build
```

`build`는 컴파일, 전체 테스트, PMD, SpotBugs를 수행합니다. 테스트는 Testcontainers가 MySQL 8.4를 직접 실행하므로 Docker가 실행 중이어야 하며, 별도의 Compose DB 준비는 필요하지 않습니다.

```bash
./gradlew test
./gradlew check
./gradlew test --tests '클래스명'
```

Windows에서는 `./gradlew` 대신 `.\gradlew.bat`을 사용합니다.

### Gradle wrapper 동시 실행 보호

이 저장소의 `gradlew`와 `gradlew.bat`은 같은 worktree에서 Gradle을 동시에 실행하지 않도록
생성된 wrapper 스크립트에 `gradle/WorktreeGradleLock.java` 호출을 추가한 버전입니다. Gradle
wrapper를 재생성할 때 이 연결을 보존해야 합니다. 잠금 동작은 중첩 Gradle 빌드를 시작하지 않는
다음 독립 검증으로 확인합니다.

```shell
java gradle/WorktreeGradleLockVerification.java
```

## CI 이미지 흐름

- `develop` push: 서버 코드·빌드·정적 분석 설정 변경 시 `backend-dev`가 테스트와 정적 분석 후 `linux/amd64`, `linux/arm64` 이미지를 Docker Hub의 `:develop`, `:<commit-sha>` 태그로 push하고, `ec2-dev` 러너에서 해당 커밋 이미지를 `prod,dev` 프로파일로 개발 서버에 배포합니다. 서버에 개발 전용 `~/yeobaek-dev/docker-compose.yml`과 `.env`를 먼저 준비해야 합니다.
- `main` push: `backend-prod`가 운영 서버 배포를 담당합니다.
- 테스트만 변경한 PR은 `backend-pr-ci`에서 검증하며 개발·운영 자동 배포를 실행하지 않습니다. 수동 배포는 각각 `develop`, `main` 브랜치에서만 실행합니다.
- Android/API 개발자는 `:develop` 이미지를 로컬 Compose에서 사용합니다.

필요한 GitHub Actions secrets는 `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`입니다.

## 로컬 설정

`local` 프로파일로 시작하면 Hibernate가 스키마를 생성한 뒤 `src/main/resources/local/data.sql`을 자동 실행합니다. 도서·본문, 회원, 모임, 진도와 댓글이 준비되며 별도 시더 실행은 필요하지 않습니다. 기존 `ddl-auto=create` 설정에 따라 재시작할 때 DB를 재생성하고 같은 테스트 데이터를 다시 넣습니다.

SQL 초기화는 공통 설정에서 비활성화하고 `application-local.properties`에서만 활성화합니다. 시드용 Java 컴포넌트는 없으며, 기본·`prod` 프로파일에서는 로컬 SQL을 실행하지 않습니다.

DB·API 바인딩과 이미지의 기본값은 Compose 설정을 따릅니다.
실제 기기에서 접속하려면 `APP_BIND_ADDRESS`를 지정하고 호스트 주소를 사용합니다.
필요한 이미지 변경은 `BACKEND_IMAGE` 환경변수로 지정합니다. 운영 비밀번호·외부 API 키는
프로퍼티 파일에 커밋하지 않고 환경변수로 주입합니다.

### PostHog 로컬 확인

PostHog는 기본적으로 비활성화되어 있습니다. US Cloud 프로젝트에서 사용자 API 행동 이벤트를 확인하려면 프로젝트 API 키를 환경변수로 주입해 개발 서버를 시작합니다. API 키는 저장소나 채팅에 남기지 않습니다.

```bash
export POSTHOG_ENABLED=true
export POSTHOG_API_KEY='<US Cloud project API key>'
export POSTHOG_HOST='https://us.i.posthog.com'
./gradlew bootRun --args='--spring.profiles.active=local'
```

Windows PowerShell에서는 같은 터미널에 환경변수를 설정한 뒤 실행합니다.

```powershell
$env:POSTHOG_ENABLED='true'
$env:POSTHOG_API_KEY='<US Cloud project API key>'
$env:POSTHOG_HOST='https://us.i.posthog.com'
.\gradlew.bat bootRun --args='--spring.profiles.active=local'
```

서버가 정상 기동하면 Spring의 조건부 설정과 PostHog SDK 초기화가 완료된 것입니다. 실제 US Cloud 수신은 애플리케이션에 테스트 전용 이벤트 코드를 남기지 않고 아래 일회성 요청으로 별도 확인합니다. `distinct_id`는 실제 회원 식별자가 아니며, `$process_person_profile=false`로 인물 프로필을 생성하지 않습니다.

```bash
curl --request POST 'https://us.i.posthog.com/batch' \
  --header 'Content-Type: application/json' \
  --data "{\"api_key\":\"${POSTHOG_API_KEY}\",\"batch\":[{\"event\":\"backend_local_smoke_test\",\"properties\":{\"distinct_id\":\"backend-local-smoke\",\"environment\":\"local\",\"\$process_person_profile\":false}}]}"
```

PostHog의 Activity에서 `backend_local_smoke_test`를 확인한 뒤 해당 테스트 이벤트를 분석에서 제외합니다. 이 요청은 SDK 연동 전에도 사용할 수 있는 수집 API 확인용이며, 아래 백엔드 사용자 API 이벤트와 구분합니다.

확인이 끝나면 현재 셸에서 값을 제거합니다.

```bash
unset POSTHOG_ENABLED POSTHOG_API_KEY POSTHOG_HOST
```

### PostHog 백엔드 사용자 API 이벤트

다음 이벤트는 서비스가 정상 반환한 사용자 API 호출에서 기록합니다. 관리자 API는 수집하지 않습니다.

| 이벤트 | API | 개별 속성 |
|---|---|---|
| `backend_member_create` | `POST /api/members` | 없음 |
| `backend_member_delete` | `DELETE /api/members/me` | 없음 |
| `backend_blocked_members_view` | `GET /api/members/me/blocks` | `blocked_member_count` |
| `backend_member_block` | `PUT /api/members/me/blocks/{memberId}` | 없음 |
| `backend_member_unblock` | `DELETE /api/members/me/blocks/{memberId}` | 없음 |
| `backend_books_view` | `GET /api/books` | `search_used`, `result_count` |
| `backend_book_view` | `GET /api/books/{bookId}` | `book_id`, `passage_count`, `chapter_count` |
| `backend_club_create` | `POST /api/clubs` | `club_id`, `book_id` |
| `backend_club_join` | `POST /api/clubs/join` | `club_id`, `book_id` |
| `backend_club_leave` | `DELETE /api/clubs/{clubId}/members/me` | `club_id` |
| `backend_clubs_view` | `GET /api/clubs` | `club_count` |
| `backend_club_view` | `GET /api/clubs/{clubId}` | `club_id`, `book_id`, `member_count`, `has_progress`, `progress_rate`(진도가 있을 때), `book_status` |
| `backend_passages_view` | `GET /api/clubs/{clubId}/passages` | `club_id`, `from`, `to`, `passage_count` |
| `backend_progress_update` | `PUT /api/clubs/{clubId}/progress` | `club_id`, `passage_id`, `last_read_passage_sequence`, `progress_rate` |
| `backend_last_reading_view` | `GET /api/members/me/last-reading` | `has_last_reading`, 기록이 있을 때 `club_id`, `book_id`, `last_read_passage_sequence`, `progress_rate` |
| `backend_new_comment_count_view` | `GET /api/clubs/{clubId}/comments/new-count` | `club_id`, `current_passage_id`, `new_comment_count` |
| `backend_commented_sentences_view` | `GET /api/clubs/{clubId}/commented-sentences` | `club_id`, `current_passage_id`, `commented_sentence_count` |
| `backend_comments_view` | 댓글 상세 조회 POST와 deprecated GET | `club_id`, `sentence_id`, `comment_count`, `api_variant` (`explicit_post` 또는 `deprecated_get`) |
| `backend_comment_create` | `POST /api/clubs/{clubId}/sentences/{sentenceId}/comments` | `club_id`, `sentence_id`, `comment_id` |
| `backend_comment_update` | `PUT /api/comments/{commentId}` | `comment_id` |
| `backend_comment_delete` | `DELETE /api/comments/{commentId}` | `comment_id` |
| `backend_comment_report` | `POST /api/comments/{commentId}/reports` | `comment_id` |

모든 이벤트에는 `source=backend`, 활성 Spring 프로파일인 `environment`, `event_schema_version=2`, `$process_person_profile=false`가 붙습니다. `outcome`, 오류 코드, HTTP 상태, 지연 시간은 전송하지 않습니다. 회원 식별에는 내부 숫자 ID를 문자열로 변환한 `distinct_id`만 사용합니다. 닉네임, 차단 대상 회원 ID, 모임 이름, 참여 코드, 검색어 원문, 책 제목, 문장·댓글 원문은 전송하지 않습니다.

현재 운영 수집은 실제 사용자가 없는 기간의 내부 테스트 계정에만 허용합니다. 운영 서버의 `.env`에서 PostHog를 활성화하고 재기동한 뒤 대상 API를 호출하면 US Cloud 프로젝트의 Activity에서 위 이벤트를 확인할 수 있습니다. 실제 사용자 유입 전에는 `POSTHOG_ENABLED=false`로 되돌리거나 국외 이전 동의 기능을 먼저 구현해야 합니다. 회원 로그인 도입 시에는 프론트엔드와 백엔드가 같은 `distinct_id`를 사용하도록 식별 정책을 함께 정합니다.

## 관측 구성

- [Spring Boot와 Better Stack 관측 구성](docs/Better_Stack_연동.md)
- [개발 서버 전환·검증·원복 명령](docs/개발_관측_전환.md)

## 더 알아보기

- [팀의 개발 방법](docs/온보딩_프로젝트_개발_방법.md)
- 에이전트/개발 지침: `AGENTS.md`, `docs/지침/`
