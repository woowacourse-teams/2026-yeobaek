package yeobaek.backend.support;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.slf4j.event.KeyValuePair;
import org.springframework.boot.logging.logback.StructuredLogEncoder;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;
import tools.jackson.databind.json.JsonMapper;

class StructuredLoggingTest {

    @Test
    void logstashOutputContainsTraceAndStructuredFields() {
        LoggerContext context = new LoggerContext();
        context.putObject(Environment.class.getName(), new MockEnvironment());
        StructuredLogEncoder encoder = new StructuredLogEncoder();
        encoder.setContext(context);
        encoder.setFormat("logstash");
        encoder.start();
        try {
            var event = new LoggingEvent(getClass().getName(), context.getLogger("structured-test"),
                    Level.INFO, "request complete", null, null);
            event.setMDCPropertyMap(Map.of("traceId", "request-7", "memberId", "7"));
            event.addKeyValuePair(new KeyValuePair("operation", "http.request"));
            event.addKeyValuePair(new KeyValuePair("status", 400));
            String encoded = new String(encoder.encode(event), StandardCharsets.UTF_8);
            Map<?, ?> json = JsonMapper.builder().build().readValue(encoded, Map.class);

            assertThat(json.get("traceId")).isEqualTo("request-7");
            assertThat(json.get("memberId")).isEqualTo("7");
            assertThat(json.get("operation")).isEqualTo("http.request");
            assertThat(json.get("status")).isEqualTo(400);
            assertThat(json.get("level")).isEqualTo("INFO");
        } finally {
            encoder.stop();
            context.stop();
        }
    }
}
