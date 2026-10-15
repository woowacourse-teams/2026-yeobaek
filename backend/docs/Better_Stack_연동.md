# Spring Boot 관측과 Better Stack

앱은 Spring Actuator·Micrometer Tracing·OpenTelemetry로 관측한다. 로그·메트릭·트레이스는
OTLP/HTTP로 `otel-collector:4318`에 전송하고, Better Stack 인증과 외부 전송은 Collector가 맡는다.
Java Agent·Logtail 직접 전송·자체 UUID trace ID는 제거했다.

사용자 선택 배경 원문 (2026-10-14):

> 지금 뭔가 누더기같이 해결하려고 붙이고 해결하려고 우회하고 이런 느낌이 강해

이 문서는 위 원문 외의 판단이나 선택 이유를 추가하지 않는다.

## 코드와 설정

- `spring-boot-starter-actuator`, `spring-boot-starter-opentelemetry`를 사용한다.
  registry·tracing bridge·OTLP exporter는 starter의 의존성을 사용한다.
- Boot 4.1.0 BOM: Micrometer 1.17.0, Tracing 1.7.0, OTel 1.62.0.
  공식 Logback appender 2.28.0-alpha는 OTel API 1.62.0을 요구한다.
  이 appender 모듈의 배포 버전에는 `-alpha`가 붙는다.
- Datasource Micrometer 2.2.1은 Boot 4 계열이다. `jdbc.includes=QUERY`로 SQL 실행 span만
  생성한다. Agent의 Hibernate·Repository span은 재현하지 않는다.
- `RequestLoggingFilter`는 Boot의 HTTP 관측 필터(`HIGHEST_PRECEDENCE + 1`)보다 안쪽
  (`+ 2`)에서 시작·완료 로그를 기록한다. traceId/spanId를 직접 설정하지 않는다.
  인증의 memberId 설정은 유지하며 요청 진입 시 정리하고 finally에서 이전 값을 복원한다.
- `logback-spring.xml`은 구조화 콘솔과 공식 OTel appender를 각각 한 번 연결한다.
  `OpenTelemetryLoggingConfiguration`은 Spring의 `OpenTelemetry` 빈을 공식 `install` API로
  연결한다. SDK 초기화 전 로그는 appender 기본 버퍼(최대 1,000건)에서 재전송된다.
- 로그의 trace/span은 MDC 문자열을 복사하지 않고 발생 당시 활성 OTel Context에서 얻는다.
  SLF4J key/value의 숫자·불리언, `operation`, `result`, 예외 타입·메시지·스택을 보존한다.
  MDC의 memberId는 OTLP에서 문자열이며 Collector의 `transform/app_logs`가 정수로 바꿔
  기존 최종 로그의 타입을 유지한다. 추적 ID를 사후 복사하는 변환은 없다.
- 전송은 SDK batch processor와 Micrometer registry의 비동기 전송을 사용한다. Collector
  장애가 요청 처리에 결합되지 않는다. 메모리 버퍼와 재시도는 영속 보관을 보장하지 않는다.
- [공통 설정](../src/main/resources/observability.properties)은 기본 외부 전송을 끈다.
  [개발 Compose](../infra/dev/docker-compose.yml)가 외부 전송을 켜며 `prod` 프로필을 유지한다.
- Actuator는 컨테이너 내부 `127.0.0.1:8081`의 health·metrics만 노출한다. 8081을 publish하거나
  nginx로 프록시하지 않는다. 기존 `/health`는 DB 확인용 공개 헬스체크로 유지한다.

## 신호별 경로

| 입력 | Collector pipeline | 대상 |
| --- | --- | --- |
| 앱 OTLP logs | logs/app | yeobaek-dev |
| 앱 OTLP traces | traces/app | yeobaek-dev |
| 앱 OTLP metrics | metrics/app | yeobaek-dev |
| hostmetrics·mysql·sqlquery/mysql_digest | metrics/infra | infra-dev |

같은 `otlp` receiver를 infra metrics pipeline에 넣지 않는다. 파일/컨테이너 콘솔 로그 수집,
Java Agent, eBPF 자동 계측, Prometheus 재수집을 이 앱에 별도로 겹치지 않는다. 앱 로그 한 건을
콘솔과 OTLP에 각각 쓰는 것 자체는 원격 중복 수집이 아니다.

## 커넥션과 SQL 시간

| 지표 | 의미 |
| --- | --- |
| http.server.requests | HTTP 전체 요청 시간·건수 |
| jvm.memory.used 등 | JVM 메트릭 |
| hikaricp.connections.acquire | 풀에서 커넥션을 얻기까지 걸린 시간(timer) |
| hikaricp.connections.pending | 커넥션을 기다리는 스레드 수(gauge) |
| hikaricp.connections.active | 사용 중인 커넥션 수(gauge) |
| hikaricp.connections.timeout | 커넥션 획득 타임아웃 수(counter) |
| jdbc.query / query span | JDBC SQL 실행 시간 |

`acquire`에는 풀 대기가 포함되고 SQL 실행 시간은 포함되지 않는다. `connections.usage`가
있다면 커넥션을 빌린 전체 시간이며 SQL 실행 시간과 같지 않다. traceId·spanId·memberId를
메트릭 태그에 추가하지 않는다. OTLP backend의 정규화에 따라 지표명이 달라질 수 있다.
기존 MySQL digest의 누적 gauge는 같은 DB/digest의 두 시점 차이로 해석한다.

## 환경 차이와 운영 영향

개발 서버의 실제 설정은 [개발 전환 절차](개발_관측_전환.md)와 `infra/dev/`에서 관리한다.
`prod`는 DB/S3 등 런타임 설정 프로필이며 관측 환경 이름과 별개다.

공통 코드에서 Logtail 의존성·사용자 appender·`logback-better-stack.xml`을 삭제했다.
실행 중인 운영 서버와 이미지는 이번 작업으로 바뀌지 않는다. 그러나 **이 변경이 포함된 새
이미지를 운영에 배포하면 기존 직접 전송은 사용할 수 없다.** 운영에 남은
`LOGGING_CONFIG=classpath:logback-better-stack.xml`은 기동 실패를 일으키므로 제거해야 한다.
운영의 새 Collector 경로·인증·환경 이름을 준비하지 않으면 기본값상 콘솔 로그만 남고 외부
로그·메트릭·트레이스 전송은 꺼진다. 운영 Agent도 제거하지 않으면 새 계측과 중복될 수 있다. 운영 전환은 별도 작업으로 수행한다.
로컬 Compose에서도 사용하지 않는 직접 전송 환경변수를 제거했다.

## 공식 근거

- [Boot 4.1 관측](https://docs.spring.io/spring-boot/4.1/reference/actuator/observability.html)
- [Spring의 OTel starter와 Logback 연결 예시](https://spring.io/blog/2025/11/18/opentelemetry-with-spring-boot/)
- [공식 Logback appender 2.28.0 설정](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/v2.28.0/instrumentation/logback/logback-appender-1.0/library/README.md)
- [Datasource Micrometer 호환성](https://github.com/jdbc-observations/datasource-micrometer)
- [Better Stack OTLP 수신](https://betterstack.com/docs/logs/open-telemetry/)
