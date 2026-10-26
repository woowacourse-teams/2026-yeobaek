package yeobaek.backend.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(classes = ObservabilityIntegrationTest.ProbeConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"management.server.port=0",
                "management.tracing.sampling.probability=1",
                "server.tomcat.threads.max=1", "server.tomcat.threads.min-spare=1",
                "spring.datasource.hikari.maximum-pool-size=1", "spring.datasource.hikari.minimum-idle=0",
                "spring.datasource.hikari.connection-timeout=5000", "management.otlp.metrics.export.step=1s",
                "management.opentelemetry.tracing.export.schedule-delay=10ms",
                "management.opentelemetry.logging.export.schedule-delay=10ms"})
@DirtiesContext
class ObservabilityIntegrationTest {

    private static final Transport TRANSPORT = new Transport();
    private static final int UNAVAILABLE = 503;
    private static final String METRICS_PATH = "/v1/metrics";
    private static final String GZIP_ENCODING = "gzip";
    private final HttpClient client = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;
    @org.springframework.beans.factory.annotation.Value("${local.management.port}")
    private int managementPort;
    @Autowired
    private Signals signals;
    @Autowired
    private SdkTracerProvider tracerProvider;
    @Autowired
    private SdkLoggerProvider loggerProvider;
    @Autowired
    private MeterRegistry meters;
    @Autowired
    private javax.sql.DataSource dataSource;

    @DynamicPropertySource
    static void otlp(DynamicPropertyRegistry properties) {
        properties.add("OBSERVABILITY_ENABLED", () -> "true");
        properties.add("OBSERVABILITY_OTLP_ENDPOINT", () -> TRANSPORT.url(""));
        properties.add("spring.application.name", () -> "yeobaek-api");
        properties.add("DEPLOYMENT_ENVIRONMENT", () -> "dev");
    }

    @AfterAll
    static void stopTransport() {
        TRANSPORT.server.stop(0);
    }

    @Test
    void correlateHttpSqlAndLogsWithCurrentSpan() throws Exception {
        String traceId = "12345678901234567890123456789012";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url("/probe?member=7")))
                .header("traceparent", "00-" + traceId + "-1234567890123456-01").build();
        assertThat(client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(200);
        flush();

        // 응답 수신 시점에는 서버 span이 아직 종료·수집되지 않았을 수 있다.
        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            var http = signals.spans.stream().filter(span -> span.getTraceId().equals(traceId)
                    && span.getKind() == io.opentelemetry.api.trace.SpanKind.SERVER).toList();
            var sql = signals.spans.stream().filter(span -> span.getTraceId().equals(traceId)
                    && span.getName().contains("query")).toList();
            assertThat(http).hasSize(1);
            assertThat(sql).hasSize(1)
                    .allSatisfy(span -> assertThat(span.getParentSpanId()).isEqualTo(http.getFirst().getSpanId()));
            assertThat(signals.logs.stream().filter(log -> log.getSpanContext().getTraceId().equals(traceId)))
                    .hasSize(4).anySatisfy(log -> {
                        assertThat(log.getSpanContext().getSpanId()).isEqualTo(sql.getFirst().getSpanId());
                        assertThat(log.getBodyValue().asString()).isEqualTo("SQL probe");
                    }).anySatisfy(log -> {
                        assertThat(log.getBodyValue().asString()).isEqualTo("HTTP 요청 처리를 완료했습니다.");
                        assertThat(log.getSpanContext().getSpanId()).isEqualTo(http.getFirst().getSpanId());
                    });
        });
    }

    @Test
    void preserveTypedFieldsExceptionAndLogsWithoutTrace() throws Exception {
        assertThat(get("/probe?member=9&fail=true").statusCode()).isEqualTo(500);
        LoggerFactory.getLogger("probe").info("outside request probe");
        flush();

        assertThat(signals.logs).anySatisfy(log -> {
            assertThat(log.getBodyValue().asString()).isEqualTo("typed probe");
            assertThat(log.getAttributes().get(AttributeKey.longKey("count"))).isEqualTo(3L);
            assertThat(log.getAttributes().get(AttributeKey.booleanKey("cached"))).isFalse();
            assertThat(log.getAttributes().get(AttributeKey.stringKey("memberId"))).isEqualTo("9");
        }).anySatisfy(log -> {
            assertThat(log.getAttributes().get(AttributeKey.stringKey("exception.type")))
                    .isEqualTo(IllegalStateException.class.getName());
            assertThat(log.getAttributes().get(AttributeKey.stringKey("exception.stacktrace")))
                    .contains("probe failure");
        }).anySatisfy(log -> {
            assertThat(log.getBodyValue().asString()).isEqualTo("outside request probe");
            assertThat(log.getSpanContext().isValid()).isFalse();
        });
    }

    @Test
    void cleanContextBetweenSuccessAndFailureRequests() throws Exception {
        flush();
        long before = signals.logs.stream().filter(log -> "probe entry".equals(log.getBodyValue().asString())).count();
        get("/probe?member=7");
        assertThat(get("/probe").body()).contains("\"previousMember\":\"none\"");
        get("/probe?member=9&fail=true");
        assertThat(get("/probe").body()).contains("\"previousMember\":\"none\"");
        flush();
        assertThat(signals.logs.stream().filter(log -> "probe entry".equals(log.getBodyValue().asString())))
                .allSatisfy(log -> assertThat(log.getSpanContext().isValid()).isTrue());
        assertThat(signals.logs.stream().filter(log -> "probe entry".equals(log.getBodyValue().asString()))
                .skip(before).map(log -> log.getSpanContext().getTraceId())).hasSize(4).doesNotHaveDuplicates();
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    void collectHttpJvmAndHikariWithoutIdentityTags() throws Exception {
        get("/probe");
        assertThat(meters.get("http.server.requests").timers()).isNotEmpty();
        assertThat(meters.get("jvm.memory.used").gauges()).isNotEmpty();
        assertThat(meters.get("hikaricp.connections.acquire").timer().count()).isPositive();
        assertThat(meters.getMeters().stream().map(meter -> meter.getId().getName())).contains(
                "hikaricp.connections.pending", "hikaricp.connections.active", "hikaricp.connections.timeout");
        assertThat(meters.getMeters()).allSatisfy(meter -> assertThat(meter.getId().getTags())
                .noneMatch(tag -> List.of("traceId", "spanId", "memberId").contains(tag.getKey())));
    }

    @Test
    void exposeTomcatWorkerPoolMetrics() throws Exception {
        assertThat(get("/probe").statusCode()).isEqualTo(200);

        assertThat(meters.get("tomcat.threads.busy").gauges())
                .anySatisfy(gauge -> assertThat(gauge.value()).isGreaterThanOrEqualTo(0));
        assertThat(meters.get("tomcat.threads.current").gauges())
                .anySatisfy(gauge -> assertThat(gauge.value()).isPositive());
        assertThat(meters.get("tomcat.threads.config.max").gauges())
                .anySatisfy(gauge -> assertThat(gauge.value()).isEqualTo(1));
    }

    @Test
    void exportAllSignalsAndKeepRequestsWorkingDuringCollectorFailure() throws Exception {
        get("/probe");
        flush();
        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(TRANSPORT.paths).contains("/v1/traces", "/v1/logs", "/v1/metrics"));
        TRANSPORT.status.set(UNAVAILABLE);
        try {
            assertThat(get("/probe").statusCode()).isEqualTo(200);
            flush();
            org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                    assertThat(TRANSPORT.failures.get()).isPositive());
            assertThat(get("/probe?fail=true").statusCode()).isEqualTo(500);
        } finally {
            TRANSPORT.status.set(200);
        }
    }

    @Test
    void exposePoolWaitingActiveAcquireAndTimeoutSeparatelyFromQuery() throws Exception {
        long previousTimeouts = (long) meters.get("hikaricp.connections.timeout").counter().count();
        try (var connection = dataSource.getConnection();
             var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            assertThat(connection.isClosed()).isFalse();
            var waiting = executor.submit(() -> {
                try (var acquired = dataSource.getConnection()) {
                    return acquired.isValid(1);
                }
            });
            // PoolStats 캐시 1초 < 관측 3초 < 연결 제한 5초 < Future 대기 7초.
            org.awaitility.Awaitility.await().pollInterval(Duration.ofMillis(10)).atMost(Duration.ofSeconds(3))
                    .untilAsserted(() -> assertThat(meters.get("hikaricp.connections.pending").gauge().value())
                            .isEqualTo(1));
            assertThat(meters.get("hikaricp.connections.active").gauge().value()).isEqualTo(1);
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> waiting.get(7, TimeUnit.SECONDS))
                    .hasCauseInstanceOf(java.sql.SQLTransientConnectionException.class);
        }
        assertThat(meters.get("hikaricp.connections.timeout").counter().count()).isGreaterThan(previousTimeouts);
    }

    @Test
    void exposeManagementOnlyOnSeparatePortAndShareSignalResource() throws Exception {
        assertThat(get("/actuator/metrics").statusCode()).isNotEqualTo(200);
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + managementPort + "/actuator/metrics"))
                .build();
        assertThat(client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(200);
        get("/probe");
        flush();
        assertThat(signals.spans).allSatisfy(span -> {
            assertThat(span.getResource().getAttribute(AttributeKey.stringKey("service.name"))).isEqualTo("yeobaek-api");
            assertThat(span.getResource().getAttribute(AttributeKey.stringKey("deployment.environment.name")))
                    .isEqualTo("dev");
        });
        assertThat(signals.logs).allSatisfy(log -> {
            assertThat(log.getResource().getAttribute(AttributeKey.stringKey("service.name"))).isEqualTo("yeobaek-api");
            assertThat(log.getResource().getAttribute(AttributeKey.stringKey("deployment.environment.name")))
                    .isEqualTo("dev");
        });
        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(TRANSPORT.metricBodies).anySatisfy(body -> assertThat(body)
                        .contains("service.name", "yeobaek-api", "deployment.environment.name", "dev")));
    }

    private void flush() {
        tracerProvider.forceFlush().join(5, TimeUnit.SECONDS);
        loggerProvider.forceFlush().join(5, TimeUnit.SECONDS);
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url(path))).timeout(Duration.ofSeconds(5)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @Import({TestcontainersConfiguration.class, RequestLoggingFilter.class,
            yeobaek.backend.support.logging.OpenTelemetryLoggingConfiguration.class,
            GlobalExceptionHandler.class, ProbeController.class})
    static class ProbeConfiguration {
        @Bean
        Signals signals() {
            return new Signals();
        }

        @Bean
        SpanExporter spanCapture(Signals signals) {
            return new SpanExporter() {
                @Override
                public CompletableResultCode export(Collection<SpanData> spans) {
                    signals.spans.addAll(spans);
                    return CompletableResultCode.ofSuccess();
                }
                @Override
                public CompletableResultCode flush() { return CompletableResultCode.ofSuccess(); }
                @Override
                public CompletableResultCode shutdown() { return CompletableResultCode.ofSuccess(); }
            };
        }

        @Bean
        LogRecordExporter logCapture(Signals signals) {
            return new LogRecordExporter() {
                @Override
                public CompletableResultCode export(Collection<LogRecordData> logs) {
                    signals.logs.addAll(logs);
                    return CompletableResultCode.ofSuccess();
                }
                @Override
                public CompletableResultCode flush() { return CompletableResultCode.ofSuccess(); }
                @Override
                public CompletableResultCode shutdown() { return CompletableResultCode.ofSuccess(); }
            };
        }

        @Bean
        @Order(1001)
        QueryExecutionListener queryLogProbe() {
            return new QueryExecutionListener() {
                @Override
                public void beforeQuery(ExecutionInfo execution, List<QueryInfo> queries) {
                    LoggerFactory.getLogger("probe").info("SQL probe");
                }
                @Override
                public void afterQuery(ExecutionInfo execution, List<QueryInfo> queries) { }
            };
        }
    }

    static class Signals {
        final List<SpanData> spans = new CopyOnWriteArrayList<>();
        final List<LogRecordData> logs = new CopyOnWriteArrayList<>();
    }

    @RestController
    static class ProbeController {
        private final JdbcTemplate jdbc;

        ProbeController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

        @GetMapping("/probe")
        Map<String, String> probe(@RequestParam(required = false) String member,
                                 @RequestParam(defaultValue = "false") boolean fail) {
            String previousMember = MDC.get("memberId");
            LoggerFactory.getLogger("probe").info("probe entry");
            if (member != null) {
                MDC.put("memberId", member);
            }
            jdbc.queryForObject("SELECT 1", Integer.class);
            if (fail) {
                LoggerFactory.getLogger("probe").atError().addKeyValue("operation", "probe")
                        .addKeyValue("result", "failure").addKeyValue("count", 3).addKeyValue("cached", false)
                        .setCause(new IllegalStateException("probe failure")).log("typed probe");
                throw new IllegalStateException("probe failure");
            }
            return Map.of("previousMember", previousMember == null ? "none" : previousMember);
        }
    }

    static final class Transport {
        final HttpServer server;
        final AtomicInteger status = new AtomicInteger(200);
        final AtomicInteger failures = new AtomicInteger();
        final List<String> paths = new CopyOnWriteArrayList<>();
        final List<String> metricBodies = new CopyOnWriteArrayList<>();

        Transport() {
            try {
                server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
                server.createContext("/", exchange -> {
                    byte[] body = exchange.getRequestBody().readAllBytes();
                    if (body.length > 0) {
                        paths.add(exchange.getRequestURI().getPath());
                        if (METRICS_PATH.equals(exchange.getRequestURI().getPath())) {
                            if (GZIP_ENCODING.equals(exchange.getRequestHeaders().getFirst("Content-Encoding"))) {
                                try (var compressed = new java.util.zip.GZIPInputStream(
                                        new java.io.ByteArrayInputStream(body))) {
                                    body = compressed.readAllBytes();
                                }
                            }
                            metricBodies.add(new String(body, java.nio.charset.StandardCharsets.ISO_8859_1));
                        }
                    }
                    int responseStatus = status.get();
                    if (responseStatus == UNAVAILABLE) {
                        failures.incrementAndGet();
                    }
                    exchange.sendResponseHeaders(responseStatus, 0);
                    exchange.close();
                });
                server.start();
            } catch (IOException exception) {
                throw new IllegalStateException(exception);
            }
        }

        String url(String signal) {
            return "http://localhost:" + server.getAddress().getPort() + signal;
        }
    }
}
