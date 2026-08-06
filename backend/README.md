# 여백 백엔드

온라인 교환독서 플랫폼 "여백"의 백엔드 서버입니다. (Java 21 / Spring Boot / MySQL)

## 사전 요구사항

| 도구 | 버전 | 용도 |
|---|---|---|
| JDK | 21 | 빌드·실행 (Gradle 툴체인이 Java 21을 요구하므로 로컬에 설치되어 있어야 한다) |
| Docker | - | 로컬 MySQL(docker compose) 및 테스트(Testcontainers) 실행 |

Gradle은 별도 설치가 필요 없다. 저장소에 포함된 래퍼(`gradlew`, `gradlew.bat`)를 사용한다.
아래 명령은 모두 `backend/` 디렉터리에서 실행한다. (Windows에서는 `./gradlew` 대신 `gradlew.bat`)

## 1. 빌드 방법

```bash
./gradlew build
```

- 컴파일, 전체 테스트, 정적 분석(PMD·SpotBugs)이 모두 수행된다. 커밋 전 게이트도 이 명령 하나로 통과 여부를 확인한다 (`docs/지침/커밋_전_게이트.md` 참고).
- 테스트가 Testcontainers로 MySQL 컨테이너를 띄우므로 **Docker가 실행 중이어야 한다.**
- 배포용 jar만 필요하면 `./gradlew bootJar`. 이때 OpenAPI 스펙(`openapi3` 태스크)이 자동 생성되어 jar 안에 동봉된다.

## 2. 실행 방법

로컬 실행은 `local` 프로파일을 사용하며, docker compose로 띄운 MySQL에 접속한다.

```bash
# 1) 로컬 MySQL 기동 (최초 1회 이후에는 컨테이너가 살아있으면 생략 가능)
docker compose up -d

# 2) API 문서(Swagger UI)가 읽을 OpenAPI 스펙 생성 — 문서 확인이 필요할 때만
./gradlew openapi3

# 3) 애플리케이션 실행
./gradlew bootRun --args='--spring.profiles.active=local'
```

- 서버: http://localhost:8080
- API 문서(Swagger UI): http://localhost:8080/docs/index.html
  - 스펙 파일(`build/api-spec/openapi3.yaml`)은 테스트의 REST Docs 스니펫으로부터 생성되므로, 문서가 비어 보이면 `./gradlew openapi3`를 먼저 실행한다.
- `local` 프로파일은 `spring.jpa.hibernate.ddl-auto=create`로 기동 시마다 스키마를 새로 만든다. 데이터를 유지해야 하는 작업에는 주의한다.

로컬 DB 접속 정보 (`docker-compose.yml`과 `application-local.properties`에 정의):

| 항목 | 값 |
|---|---|
| 호스트 | `localhost:3306` |
| 데이터베이스 | `yeobaek` |
| 사용자 / 비밀번호 | `root` / `yeobaek` |

## 3. 테스트 방법

```bash
# 전체 테스트
./gradlew test

# 테스트 + 정적 분석(PMD·SpotBugs)까지
./gradlew check
```

- 리포지토리·통합 테스트는 Testcontainers가 MySQL 8.4 컨테이너를 직접 띄워 수행하므로 **Docker가 실행 중이어야 한다.** 별도의 DB 준비(스키마 생성, docker compose 기동)는 필요 없다.
- 전체 테스트는 컨테이너 기동 포함 수 분이 걸릴 수 있다. 개발 중에는 `./gradlew test --tests '클래스명'`으로 범위를 좁혀 실행한다.

## 4. 환경변수

**현재 필수 환경변수는 없다.** 로컬 개발에 필요한 설정(DB 접속 정보 등)은 모두 `application-local.properties`와 `docker-compose.yml`에 커밋되어 있고, 비밀값이 아직 존재하지 않기 때문이다.

유일하게 지정이 필요한 것은 **활성 프로파일**이며, 다음 중 편한 방법으로 주입한다.

| 방법 | 예시 |
|---|---|
| 커맨드라인 인자 | `./gradlew bootRun --args='--spring.profiles.active=local'` |
| 환경변수 | `SPRING_PROFILES_ACTIVE=local ./gradlew bootRun` |
| IntelliJ 실행 구성 | Run Configuration → Active profiles에 `local` 입력 |

앞으로 비밀값(운영 DB 비밀번호, 외부 API 키 등)이 생기면 프로퍼티 파일에 커밋하지 않고 환경변수로 주입한다. Spring의 relaxed binding 규칙에 따라 프로퍼티 키를 대문자·언더스코어로 바꾼 환경변수가 자동 매핑된다 (예: `spring.datasource.password` → `SPRING_DATASOURCE_PASSWORD`). 새 환경변수를 추가할 때는 이 문서의 목록도 함께 갱신한다.

## 더 알아보기

- 에이전트/개발 지침: `CLAUDE.md`, `docs/지침/`
- 개발 계획: `docs/개발계획.md`
