# Better Stack appender 연결

Better Stack의 공식 `com.logtail:logback-logtail:0.4.0`을 사용한다. 기본 실행은 기존 Spring Boot
로깅을 유지하며, 아래 환경변수로 별도 설정 파일을 선택한 실행에서만 HTTP 전송을 활성화한다.

| 환경변수 | 값 |
| --- | --- |
| `LOGGING_CONFIG` | `classpath:logback-better-stack.xml` |
| `BETTER_STACK_SOURCE_TOKEN` | Better Stack source 설정의 Source token |
| `BETTER_STACK_INGEST_URL` | 해당 source의 ingesting host에 `https://`를 붙인 전체 URL |

토큰을 저장소나 실행 명령에 적지 않고 실행 환경의 secret으로 주입한다. 운영 Compose에서는
서버의 `.env` 또는 호스트 환경에 세 값을 설정하고 app 컨테이너를 재생성한다.
`backend/infra/prod/docker-compose.prod.yml`이 세 변수를 컨테이너에 전달한다.
해제하려면 `LOGGING_CONFIG`를 제거하고 재시작한다. 앱 로그 한 건을 발생시킨 뒤 Better Stack
Live tail에서 수신을 확인한다. 콘솔 출력만으로 원격 수신 성공을 판정하지 않는다.

## PR #207과의 연결

- 애플리케이션 로깅 코드는 변경하지 않는다. PR #207 병합 전후 모두 설정 파일을 사용할 수 있다.
- 콘솔은 Spring Boot의 `StructuredLogEncoder`를 사용한다. `logging.structured.format.console`을
  따르며, 설정이 없는 병합 전에도 이 연결 설정에서는 `logstash`를 기본값으로 사용한다.
- `traceId`와 `memberId`는 각각 `meta.traceId`(문자열), `meta.memberId`(정수)로 전송한다.
  MDC가 없는 시작·백그라운드 로그에는 해당 필드가 없다.
- `StructuredLogtailAppender`는 공식 appender의 전송 JSON에 SLF4J `addKeyValue` 값을
  최상위 필드로 추가한다. `operation`, `result`, `enabled`, `eventName` 등을 필드명으로
  조회할 수 있고 숫자·불리언 값은 원래 타입으로 전송한다. appender가 이미 사용하는
  `dt`, `level`, `app`, `message`, `meta`, `runtime`, `args`, `throwable`과 이름이 같은
  키는 기존 전송 필드를 덮어쓰지 않는다.
- 원격 `message`에는 로그 문장만 전송한다. 콘솔 JSON에는 기존 구조화 필드가 그대로 유지된다.
- 예외는 appender의 `throwable` 필드로 전송한다. `%nopex`는 메시지에 스택을 중복 출력하지 않는다.

배치·재시도·종료 시 flush는 공식 appender 기본값을 사용한다. 전송 성공을 보장하는 영속 큐는
없다. 토큰이나 URL을 비워 두면 첫 로그에서 appender가 전송을 비활성화하므로, 활성화할 때
세 환경변수를 모두 주입한다.

## 근거

- [Better Stack Java 공식 설정](https://betterstack.com/docs/logs/java/)
- [공식 appender 소스](https://github.com/logtail/logback-logtail)
- [Maven Central 배포 버전](https://repo.maven.apache.org/maven2/com/logtail/logback-logtail/0.4.0/)
- [Spring Boot 로깅 설정](https://docs.spring.io/spring-boot/reference/features/logging.html)

공식 안내의 예시 버전은 0.3.4이다. 이번 설정은 2026-09-30 Maven Central에서 확인한 0.4.0을
고정한다. 0.4.0은 전송 전에 MDC·메시지·스레드를 캡처하고 종료 시 flush 대기에 상한을 둔다.
Logback·SLF4J 버전은 Spring Boot의 의존성 관리를 유지한다.
