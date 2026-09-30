package yeobaek.backend.support.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.LoggerContext;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.boot.logging.LoggingSystemProperty;
import org.springframework.boot.logging.logback.LogbackLoggingSystem;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.mock.env.MockEnvironment;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(OutputCaptureExtension.class)
class BetterStackLoggingConfigurationTest {

    private final LogbackLoggingSystem loggingSystem = new LogbackLoggingSystem(
            Thread.currentThread().getContextClassLoader());
    private final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    private final Map<String, String> originalProperties = new HashMap<>();
    private HttpServer server;

    @BeforeEach
    void captureLoggingProperties() {
        for (LoggingSystemProperty property : LoggingSystemProperty.values()) {
            String name = property.getEnvironmentVariableName();
            originalProperties.put(name, System.getProperty(name));
        }
        originalProperties.put("LOGGED_APPLICATION_NAME", System.getProperty("LOGGED_APPLICATION_NAME"));
    }

    @AfterEach
    void restoreLogging() {
        MDC.clear();
        loggingSystem.cleanUp();
        loggerContext.stop();
        if (server != null) {
            server.stop(0);
        }
        restoreLoggingProperties();
        loggingSystem.initialize(new LoggingInitializationContext(new MockEnvironment()), null, null);
        restoreLoggingProperties();
    }

    private void restoreLoggingProperties() {
        originalProperties.forEach((name, value) -> {
            if (value == null) {
                System.clearProperty(name);
            } else {
                System.setProperty(name, value);
            }
        });
    }

    @Test
    @DisplayName("연결 설정을 선택하지 않은 기본 실행은 Better Stack으로 전송하지 않는다")
    void defaultLoggingDoesNotAttachBetterStack() {
        initialize(new MockEnvironment(), null);

        var root = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        assertThat(root.getAppender("BETTER_STACK")).isNull();
        assertThat(root.getAppender("CONSOLE")).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "logstash"})
    @DisplayName("PR 207의 설정 유무에 관계없이 환경변수로 연결하고 MDC와 실행 필드를 보존한다")
    void sendsLogAndPreservesStructuredConsole(String consoleFormat, CapturedOutput output) throws Exception {
        var requests = new LinkedBlockingQueue<Map<String, String>>();
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            try (exchange) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                requests.add(Map.of("authorization", exchange.getRequestHeaders().getFirst("Authorization"),
                        "body", body));
                exchange.sendResponseHeaders(202, -1);
            }
        });
        server.start();
        var environment = new MockEnvironment().withProperty("spring.application.name", "backend-test");
        environment.getPropertySources().addFirst(new SystemEnvironmentPropertySource("test-env", Map.of(
                "BETTER_STACK_SOURCE_TOKEN", "test-source-token",
                "BETTER_STACK_INGEST_URL", "http://localhost:" + server.getAddress().getPort())));
        if (!consoleFormat.isEmpty()) {
            environment.setProperty("logging.structured.format.console", consoleFormat);
        }
        initialize(environment, "classpath:logback-better-stack.xml");
        MDC.put("traceId", "request-123");
        MDC.put("memberId", "42");
        MDC.put("unlisted", "must-not-be-forwarded-as-meta");
        loggerContext.getLogger("better-stack-test").atInfo()
                .addKeyValue("operation", "club.join")
                .addKeyValue("result", "success")
                .log("모임 참여 완료");
        MDC.clear();

        Map<String, String> request = requests.poll(10, TimeUnit.SECONDS);
        assertThat(request).isNotNull().containsEntry("authorization", "Bearer test-source-token");
        var log = JsonMapper.builder().build().readTree(request.get("body")).get(0);
        assertThat(log.path("meta").toString())
                .contains("\"traceId\":\"request-123\"", "\"memberId\":42")
                .doesNotContain("unlisted");
        assertThat(log.path("app").asString()).isEqualTo("backend-test");
        assertThat(log.path("message").asString())
                .contains("모임 참여 완료", "operation=\"club.join\"", "result=\"success\"");
        assertThat(output.getOut()).contains("\"traceId\":\"request-123\"", "\"operation\":\"club.join\"",
                "\"result\":\"success\"");
    }

    private void initialize(MockEnvironment environment, String location) {
        loggingSystem.cleanUp();
        loggingSystem.initialize(new LoggingInitializationContext(environment), location, null);
    }
}
