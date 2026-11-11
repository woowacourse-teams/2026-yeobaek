# Spring Boot 관측과 Better Stack

앱은 Spring Actuator·Micrometer Tracing·OpenTelemetry로 관측한다. 로그·메트릭·트레이스는
OTLP/HTTP로 `otel-collector:4318`에 전송하고, Better Stack 인증과 외부 전송은 Collector가 맡는다.

## 코드와 설정

- `spring-boot-starter-actuator`, `spring-boot-starter-opentelemetry`를 사용한다.
  registry·tracing bridge·OTLP exporter는 starter의 의존성을 사용한다.
- Boot 4.1.0 BOM: Micrometer 1.17.0, Tracing 1.7.0, OTel 1.62.0.
  공식 Logback appender 2.28.0-alpha는 OTel API 1.62.0을 요구한다.
  이 appender 모듈의 배포 버전에는 `-alpha`가 붙는다.
- Datasource Micrometer 2.2.1은 Boot 4 계열이다. `jdbc.includes=QUERY`로 SQL 실행 span만
  생성한다. Hibernate·Repository 메서드 단위 span은 수집 범위에 포함하지 않는다.
- `RequestLoggingFilter`는 Boot의 HTTP 관측 필터(`HIGHEST_PRECEDENCE + 1`)보다 안쪽
  (`+ 2`)에서 시작·완료 로그를 기록한다. traceId/spanId를 직접 설정하지 않는다.
  인증의 memberId 설정은 유지하며 요청 진입 시 정리하고 finally에서 이전 값을 복원한다.
- `logback-spring.xml`은 구조화 콘솔과 공식 OTel appender를 각각 한 번 연결한다.
  `OpenTelemetryLoggingConfiguration`은 Spring의 `OpenTelemetry` 빈을 공식 `install` API로
  연결한다. SDK 초기화 전 로그는 appender 기본 버퍼(최대 1,000건)에서 재전송된다.
- 로그의 trace/span은 MDC 문자열을 복사하지 않고 발생 당시 활성 OTel Context에서 얻는다.
  SLF4J key/value의 숫자·불리언, `operation`, `result`, 예외 타입·메시지·스택을 보존한다.
  MDC의 memberId는 OTLP에서 문자열이며 Collector의 `transform/app_logs`가 정수로 바꿔
  Better Stack에 정수 타입으로 전송한다.
- 전송은 SDK batch processor와 Micrometer registry의 비동기 전송을 사용한다. Collector
  장애가 요청 처리에 결합되지 않는다. 메모리 버퍼와 재시도는 영속 보관을 보장하지 않는다.
- [공통 설정](../src/main/resources/observability.properties)은 기본 외부 전송을 끈다.
  [개발 Compose](../infra/dev/docker-compose.dev.yml)가 외부 전송을 켜며 `prod,dev` 프로필을 사용한다.
- Actuator는 컨테이너 내부 `127.0.0.1:8081`의 health·metrics만 노출한다. 8081을 publish하거나
  nginx로 프록시하지 않는다. `/health`는 DB 확인용 공개 헬스체크다.

## 신호별 경로

| 입력 | Collector pipeline | 대상 |
| --- | --- | --- |
| 앱 OTLP logs | logs/app | 해당 환경의 앱 소스 |
| 앱 OTLP traces | traces/app | 해당 환경의 앱 소스 |
| 앱 OTLP metrics | metrics/app | 해당 환경의 앱 소스 |
| hostmetrics·mysql·SQL digest receiver | metrics/infra | 해당 환경의 인프라 소스 |

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
MySQL digest의 누적 gauge는 같은 DB/digest의 두 시점 차이로 해석한다.

## 환경별 설정

개발은 [개발 Compose](../infra/dev/docker-compose.dev.yml)와
[개발 Collector](../infra/dev/otel-collector.yaml), 운영은
[운영 Compose](../infra/prod/docker-compose.prod.yml)와
[운영 Collector](../infra/prod/otel-collector.yaml)를 사용한다.
파일 배치·적용·백업·복구는 [배포 가이드](배포_가이드.md)를 따른다.

- 개발 프로필은 `prod,dev`, 운영은 `prod`다. `prod`는 DB/S3 설정 프로필이며 관측 환경과 별개다.
- 두 환경 모두 `OBSERVABILITY_ENABLED=true`, `OBSERVABILITY_OTLP_ENDPOINT=http://otel-collector:4318`을 사용한다.
  `DEPLOYMENT_ENVIRONMENT`는 각각 `dev`, `prod`로 구분한다.
- `.env`는 Compose 값 치환용이다. 앱에는 Compose의 `app.environment`에 지정된 값만 전달된다.
- `otel.env`는 Collector 전용이다. `BETTER_STACK_APP_*`는 앱 소스,
  `BETTER_STACK_INFRA_*`는 인프라 소스의 URL·토큰이며 환경별 실제 값은 서로 다르다.
  endpoint는 `/v1/*`를 붙이지 않은 OTLP base URL이다. 앱에는 Better Stack 토큰을 전달하지 않는다.
- `MYSQL_MONITOR_PASSWORD`는 `otel_monitor` 계정의 비밀번호다. 계정·권한은 별도 준비하고,
  MySQL CPU 계측 활성화는 배포 가이드의 SQL과 시작 옵션으로 관리한다.

## 배포 후 수신 확인

대상 EC2에서 실행한다. 운영은 `~/yeobaek`, 프로젝트 `yeobaek`으로 두 줄을 바꾼다.

```bash
cd ~/yeobaek-dev
COMPOSE=(docker compose -p yeobaek-dev -f docker-compose.yml)
"${COMPOSE[@]}" ps
"${COMPOSE[@]}" logs --since=5m otel-collector
curl --fail http://127.0.0.1:8080/health
APP_ID=$("${COMPOSE[@]}" ps -q app)
test -n "$APP_ID"
docker run --rm --network "container:$APP_ID" curlimages/curl:8.16.0 \
  -fsS http://127.0.0.1:8081/actuator/health
for metric in hikaricp.connections.acquire hikaricp.connections.pending hikaricp.connections.active hikaricp.connections.timeout; do
  docker run --rm --network "container:$APP_ID" curlimages/curl:8.16.0 \
    -fsS "http://127.0.0.1:8081/actuator/metrics/$metric"
done
```

Collector가 실행 중이며 401/403·TLS·DB 연결·export 오류가 반복되지 않는지 확인한다.
시작 로그는 기동 시에만 나오므로 최근 로그에 `Everything is ready`가 없다는 이유만으로
장애로 판단하지 않는다. 풀 메트릭은 DB를 사용하는 API 요청 후 확인하고,
정상 상태에서 pending·timeout이 0인 것은 정상이다. 관리 포트 8081·4318·8888은 외부에 공개하지 않는다.

### 개발 서버에서 로그와 trace 연결 확인

아래 요청은 개발 서버의 테스트 회원과 `X-Member-Id` 인증을 사용한다. 운영에서는 실제 앱의
인증 흐름으로 조회 요청을 보내고 그 요청의 trace를 확인한다.

```bash
MEMBER_ID='<개발 테스트 회원 ID>'
TRACE_OK=$(openssl rand -hex 16)
TRACE_FAIL=$(openssl rand -hex 16)
PARENT_SPAN=$(openssl rand -hex 8)
curl --fail -H "X-Member-Id: $MEMBER_ID" \
  -H "traceparent: 00-$TRACE_OK-$PARENT_SPAN-01" http://127.0.0.1:8080/api/books
curl -sS -w '\nHTTP %{http_code}\n' -H "X-Member-Id: $MEMBER_ID" \
  -H "traceparent: 00-$TRACE_FAIL-$PARENT_SPAN-01" http://127.0.0.1:8080/api/books/not-a-number
curl --fail http://127.0.0.1:8080/health
printf '정상 trace: %s\n실패 trace: %s\n' "$TRACE_OK" "$TRACE_FAIL"
```

정상 요청 200, 형식 오류 요청 400, 후속 health 200을 확인한다. 형식 오류 400에는
예상하지 못한 예외의 stack trace를 요구하지 않는다.

### Better Stack에서 확인할 항목

수집 주기(기본 1분) 이상 기다리고 최근 시간 범위를 선택한다. Collector 실행 상태나
로컬 메트릭 조회 성공만으로 Better Stack 수신 성공을 판정하지 않는다.

- 앱 소스에서 요청 로그와 HTTP/SQL span을 같은 원본 OTLP trace ID로 찾을 수 있다.
  요청 시작·완료 로그는 HTTP server span에 연결되고 후속 익명 health에는 memberId가 남지 않는다.
- `operation`, `result`, `memberId`, `resultCount`(숫자), `searchUsed`(불리언) 등 해당 로그의
  필드 타입이 유지된다. 예외가 발생했다면 타입·메시지·스택을 확인한다. 시작 로그의 빈 trace는 정상이다.
- 같은 trace의 HTTP server span이나 SQL span이 계측 때문에 중복 생성되지 않는다.
  요청 시작·완료 두 로그와 같은 이벤트의 중복 수집을 구분한다.
- 앱 소스에서 HTTP·JVM·Hikari 메트릭이 갱신되고, 인프라 소스에서 EC2·MySQL·`mysql.query.*`가 갱신된다.
  앱 OTLP metrics는 인프라 소스로 보내지 않는다. 과거 수집 데이터와 현재 전송을 구분한다.
- traceId·memberId를 메트릭 태그로 넣지 않는다. SQL digest의 누적 값은 같은 DB/digest의 차이로 해석하고,
  MySQL 재시작이나 통계 초기화를 가로지르는 구간은 비교하지 않는다.

Collector self metrics를 추가 확인하려면 대상 서버에서 다음을 실행한다.

```bash
docker run --rm --network container:yeobaek-otel-collector curlimages/curl:8.16.0 \
  -fsS http://127.0.0.1:8888/metrics | grep -E 'otelcol_.*(accepted|sent|failed|refused)'
```

요청·수집 전후 accepted/sent 증가와 refused/send_failed의 지속 증가 여부를 확인한다.
수신이 확인되지 않으면 Collector 오류, `otel.env`의 대상 소스·인증 정보, MySQL 계정 권한을 점검한다.
비밀값을 포함한 환경 파일이나 전체 컨테이너 환경은 로그·PR에 붙이지 않는다.

## 공식 근거

- [Boot 4.1 관측](https://docs.spring.io/spring-boot/4.1/reference/actuator/observability.html)
- [Spring의 OTel starter와 Logback 연결 예시](https://spring.io/blog/2025/11/18/opentelemetry-with-spring-boot/)
- [공식 Logback appender 2.28.0 설정](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/v2.28.0/instrumentation/logback/logback-appender-1.0/library/README.md)
- [Datasource Micrometer 호환성](https://github.com/jdbc-observations/datasource-micrometer)
- [Better Stack OTLP 수신](https://betterstack.com/docs/logs/open-telemetry/)
