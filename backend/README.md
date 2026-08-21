# 여백 백엔드

온라인 교환독서 플랫폼 "여백"의 백엔드 서버입니다. (Java 21 / Spring Boot / MySQL)

> 팀에 새로 합류한 개발자는 [온보딩 문서](docs/온보딩_프로젝트_개발_방법.md)부터 읽어야 합니다.

## 가장 빠른 로컬 API 테스트 (Android/API 개발자)

Docker만 설치하면 `develop` 브랜치에서 CI가 미리 빌드한 백엔드와 MySQL을 함께 실행할 수 있습니다. JDK는 필요하지 않습니다. macOS/Linux 스크립트는 백엔드 HTTP 준비 확인에 기본 `curl` 명령을 사용합니다.

최초 한 번, Docker Hub 이미지 경로를 설정합니다.

```powershell
# Windows PowerShell
Copy-Item .env.local.example .env.local
# .env.local의 YOUR_DOCKERHUB_USERNAME을 실제 팀 계정으로 변경
```

```bash
# macOS/Linux
cp .env.local.example .env.local
# .env.local의 YOUR_DOCKERHUB_USERNAME을 실제 팀 계정으로 변경
```

이후에는 아래 단일 명령으로 관리합니다.

| 작업 | Windows PowerShell | macOS/Linux |
|---|---|---|
| 서버+DB 시작 | `.\local-env.ps1 up` | `sh ./local-env.sh up` |
| 서버+DB 종료 | `.\local-env.ps1 down` | `sh ./local-env.sh down` |
| 상태 확인 | `.\local-env.ps1 status` | `sh ./local-env.sh status` |
| 로그 확인 | `.\local-env.ps1 logs` | `sh ./local-env.sh logs` |

`up`은 최신 `develop` 이미지를 pull하고 MySQL healthcheck와 백엔드 HTTP 응답까지 기다립니다. `down`은 서버와 DB를 함께 종료하고 로컬 DB 데이터도 정리합니다. 기존 `local` 프로파일의 `ddl-auto=create` 정책을 유지하므로 다음 기동은 항상 깨끗한 스키마에서 시작합니다.

- 서버: http://localhost:8080
- Swagger UI: http://localhost:8080/docs
- OpenAPI JSON: http://localhost:8080/v3/api-docs

`BACKEND_IMAGE가 설정되지 않았습니다` 오류가 나오면 `.env.local`이 존재하고 다음 형태인지 확인합니다.

```dotenv
BACKEND_IMAGE=팀_DOCKERHUB_계정/yeobaek-backend:develop
```

## 백엔드 코드 개발

백엔드 개발자는 DB만 Docker로 실행하고 애플리케이션은 호스트의 Gradle로 실행할 수 있습니다. JDK 21과 Docker가 필요하며 Gradle은 저장소의 Wrapper를 사용합니다.

```powershell
# Windows: DB 시작 → bootRun. Ctrl+C 또는 프로세스 종료 시 DB 컨테이너 정리
.\local-env.ps1 dev
```

```bash
# macOS/Linux: DB 시작 → bootRun. Ctrl+C 또는 프로세스 종료 시 DB 컨테이너 정리
sh ./local-env.sh dev
```

로컬 서버는 `local` 프로파일로 실행됩니다. `dev`는 기존 API 컨테이너가 실행 중이면 먼저 중지해 8080 포트 충돌을 막습니다. IDE에서 서버를 실행할 때는 `.\local-env.ps1 db`(Windows) 또는 `sh ./local-env.sh db`(macOS/Linux)로 DB만 실행하고, 작업 후 공통 `down` 명령으로 종료합니다.

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

## CI 이미지 흐름

- `develop` push: 테스트와 정적 분석 후 `linux/amd64`, `linux/arm64` 이미지를 Docker Hub의 `:develop`, `:<commit-sha>` 태그로 push합니다. 배포 작업은 없습니다.
- `main` push: 운영 CI/CD 워크플로가 운영 서버 배포를 담당합니다.
- Android/API 개발자는 `:develop` 이미지를 로컬 Compose에서 사용합니다.

필요한 GitHub Actions secrets는 `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`입니다.

## 로컬 설정

Compose의 로컬 MySQL 기본값은 데이터베이스 `yeobaek`, 사용자 `root`, 비밀번호 `yeobaek`, 호스트 포트 `13306`입니다. 이 값은 PC에 이미 설치된 MySQL의 기본 포트 `3306`과 충돌하지 않도록 분리되어 있으며, 로컬 개발 전용입니다.

DB와 API 포트는 기본적으로 `127.0.0.1`에만 공개됩니다. 실제 Android 기기에서 같은 LAN을 통해 접속해야 할 때만 `.env.local`의 `APP_BIND_ADDRESS`를 `0.0.0.0`으로 바꾸고, 테스트가 끝나면 원래 값으로 되돌립니다. 이 경우 로컬 프로파일의 개발용 자격정보가 LAN에 노출될 수 있으므로 신뢰할 수 있는 네트워크에서만 사용합니다.

`.env.local`은 개인별 이미지 경로를 담고 Git에서 제외됩니다. 앞으로 운영 DB 비밀번호나 외부 API 키가 생기면 프로퍼티 파일에 커밋하지 않고 환경변수로 주입합니다.

## 더 알아보기

- [팀의 개발 방법](docs/온보딩_프로젝트_개발_방법.md)
- 에이전트/개발 지침: `AGENTS.md`, `docs/지침/`
- 개발 계획: `docs/개발계획.md`
