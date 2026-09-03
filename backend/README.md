# 여백 백엔드

온라인 교환독서 플랫폼 "여백"의 백엔드 서버입니다. (Java 21 / Spring Boot / MySQL)

> 팀에 새로 합류한 개발자는 [온보딩 문서](docs/온보딩_프로젝트_개발_방법.md)부터 읽어야 합니다.

## Android/API 로컬 테스트

Docker만으로 사전 빌드된 백엔드와 MySQL을 실행할 수 있으며 JDK는 필요하지 않습니다.

처음 환경을 준비하는 방법부터 Android 에뮬레이터·실제 기기에서 접속하는 방법, 종료 및 문제 해결 절차까지는 루트의 **[로컬 백엔드 테스트 환경 구성 방법](../docs/로컬_테스트_방법.md)**을 따라 진행하세요.

## 백엔드 코드 개발

백엔드 개발자는 DB만 Docker로 실행하고 애플리케이션은 호스트의 Gradle로 실행할 수 있습니다. JDK 21과 Docker가 필요하며 Gradle은 저장소의 Wrapper를 사용합니다.

```powershell
# Windows: DB 시작 → bootRun. Ctrl+C 또는 프로세스 종료 시 DB 컨테이너 정리
pwsh -NoProfile -File .\local-env.ps1 dev
```

```bash
# macOS/Linux: DB 시작 → bootRun. Ctrl+C 또는 프로세스 종료 시 DB 컨테이너 정리
sh ./local-env.sh dev
```

로컬 서버는 `local` 프로파일로 실행됩니다. `dev`는 기존 API 컨테이너가 실행 중이면 먼저 중지해 8080 포트 충돌을 막습니다. Windows 명령은 PowerShell 7.2 이상(`pwsh`)을 기준으로 합니다. IDE에서 서버를 실행할 때는 `pwsh -NoProfile -File .\local-env.ps1 db`(Windows) 또는 `sh ./local-env.sh db`(macOS/Linux)로 DB만 실행하고, 작업 후 공통 `down` 명령으로 종료합니다.

스크립트는 체크아웃 경로별 Compose 프로젝트명을 사용하므로 다른 clone이나 worktree의 컨테이너를 `down`하지 않습니다. 필요하면 `COMPOSE_PROJECT_NAME`으로 명시적으로 덮어쓸 수 있습니다.

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

- `develop` push: 테스트와 정적 분석 후 `linux/amd64`, `linux/arm64` 이미지를 Docker Hub의 `:develop`, `:<commit-sha>` 태그로 push합니다. 배포 작업은 없습니다.
- `main` push: 운영 CI/CD 워크플로가 운영 서버 배포를 담당합니다.
- Android/API 개발자는 `:develop` 이미지를 로컬 Compose에서 사용합니다.

필요한 GitHub Actions secrets는 `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`입니다.

## 로컬 설정

Compose의 로컬 MySQL 기본값은 데이터베이스 `yeobaek`, 사용자 `root`, 비밀번호 `yeobaek`, 호스트 포트 `13306`입니다. 이 값은 PC에 이미 설치된 MySQL의 기본 포트 `3306`과 충돌하지 않도록 분리되어 있으며, 로컬 개발 전용입니다.

DB와 API 포트는 기본적으로 `127.0.0.1`에만 공개됩니다. Android 에뮬레이터와 실제 기기의 접속 설정은 [로컬 백엔드 테스트 환경 구성 방법](../docs/로컬_테스트_방법.md)을 참고합니다.

기본적으로 팀의 `alstj2384/yeobaek-backend:develop` 이미지를 사용합니다. 다른 이미지나 API 바인딩 주소가 필요할 때만 `.env.local.example`을 `.env.local`로 복사해 값을 재정의합니다. `.env.local`은 Git에서 제외됩니다. 앞으로 운영 DB 비밀번호나 외부 API 키가 생기면 프로퍼티 파일에 커밋하지 않고 환경변수로 주입합니다.

### PostHog 로컬 확인

PostHog는 기본적으로 비활성화되어 있으며 사용자 행동 이벤트는 아직 정의하지 않았습니다. US Cloud 프로젝트를 연결해 SDK 초기화만 확인하려면 프로젝트 API 키를 환경변수로 주입해 개발 서버를 시작합니다. API 키는 저장소나 채팅에 남기지 않습니다.

```bash
export POSTHOG_ENABLED=true
export POSTHOG_API_KEY='<US Cloud project API key>'
export POSTHOG_HOST='https://us.i.posthog.com'
sh ./local-env.sh dev
```

Windows의 PowerShell 7.2 이상에서는 같은 터미널에 환경변수를 설정한 뒤 실행합니다.

```powershell
$env:POSTHOG_ENABLED='true'
$env:POSTHOG_API_KEY='<US Cloud project API key>'
$env:POSTHOG_HOST='https://us.i.posthog.com'
pwsh -NoProfile -File .\local-env.ps1 dev
```

서버가 정상 기동하면 Spring의 조건부 설정과 PostHog SDK 초기화가 완료된 것입니다. 실제 US Cloud 수신은 애플리케이션에 테스트 전용 이벤트 코드를 남기지 않고 아래 일회성 요청으로 별도 확인합니다. `distinct_id`는 실제 회원 식별자가 아니며, `$process_person_profile=false`로 인물 프로필을 생성하지 않습니다.

```bash
curl --request POST 'https://us.i.posthog.com/batch' \
  --header 'Content-Type: application/json' \
  --data "{\"api_key\":\"${POSTHOG_API_KEY}\",\"batch\":[{\"event\":\"backend_local_smoke_test\",\"properties\":{\"distinct_id\":\"backend-local-smoke\",\"environment\":\"local\",\"\$process_person_profile\":false}}]}"
```

PostHog의 Activity에서 `backend_local_smoke_test`를 확인한 뒤 해당 테스트 이벤트를 분석에서 제외합니다. 이 요청은 SDK 연동 전에도 사용할 수 있는 수집 API 확인용이며, 아래 백엔드 시험 이벤트와 구분합니다.

확인이 끝나면 현재 셸에서 값을 제거합니다.

```bash
unset POSTHOG_ENABLED POSTHOG_API_KEY POSTHOG_HOST
```

### PostHog 백엔드 시험 이벤트

다음 이벤트는 최종 퍼널을 확정하기 전 백엔드 API 성공 수집을 검증하기 위한 시험 이벤트입니다.

| 이벤트 | 기록 시점 | 개별 속성 |
|---|---|---|
| `backend_member_created` | 회원 생성 성공 | 없음 |
| `backend_club_created` | 모임 생성 성공 | `club_id`, `book_id` |
| `backend_club_joined` | 모임 참여 성공 | `club_id`, `book_id` |
| `backend_passages_viewed` | 본문 범위 조회 성공 | `club_id`, `from`, `to`, `passage_count` |
| `backend_comments_viewed` | 댓글이 1개 이상인 목록 조회 성공 | `club_id`, `sentence_id`, `comment_count` |
| `backend_comment_created` | 댓글 작성 성공 | `club_id`, `sentence_id`, `comment_id` |

모든 이벤트에는 `source=backend`, 활성 Spring 프로파일인 `environment`, `event_schema_version=1`, `$process_person_profile=false`가 붙습니다. 회원 식별에는 내부 숫자 ID를 문자열로 변환한 `distinct_id`만 사용하며 닉네임, 모임 이름, 참여 코드, 책 제목, 문장·댓글 원문은 전송하지 않습니다.

현재 운영 수집은 실제 사용자가 없는 기간의 내부 테스트 계정에만 허용합니다. 운영 서버의 `.env`에서 PostHog를 활성화하고 재기동한 뒤 대상 API를 호출하면 US Cloud 프로젝트의 Activity에서 위 이벤트를 확인할 수 있습니다. 실제 사용자 유입 전에는 `POSTHOG_ENABLED=false`로 되돌리거나 국외 이전 동의 기능을 먼저 구현해야 합니다.

## 더 알아보기

- [팀의 개발 방법](docs/온보딩_프로젝트_개발_방법.md)
- 에이전트/개발 지침: `AGENTS.md`, `docs/지침/`
- 개발 계획: `docs/개발계획.md`
