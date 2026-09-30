package yeobaek.backend.support.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yeobaek.backend.support.LogCapture;

class StructuredLogtailAppenderTest {

    private static final String LOGGER_NAME = StructuredLogtailAppenderTest.class.getName();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void keyValuesAreSentAsTypedTopLevelJsonFields() throws Exception {
        StructuredLogtailAppender appender = newAppender();
        try (LogCapture capture = new LogCapture(LOGGER_NAME)) {
            Logger logger = LoggerFactory.getLogger(LOGGER_NAME);
            logger.atInfo().addKeyValue("operation", "analytics.track")
                    .addKeyValue("enabled", false)
                    .addKeyValue("eventName", "backend_new_comment_count_view")
                    .addKeyValue("resultCount", 3)
                    .log("비활성화된 분석 이벤트 처리를 시작합니다.");

            String json = OBJECT_MAPPER.writeValueAsString(appender.buildPostData(capture.events().getFirst()));
            JsonNode payload = OBJECT_MAPPER.readTree(json);

            assertThat(payload.path("message").asText()).isEqualTo("비활성화된 분석 이벤트 처리를 시작합니다.");
            assertThat(payload.path("operation").asText()).isEqualTo("analytics.track");
            assertThat(payload.path("enabled")).isEqualTo(BooleanNode.FALSE);
            assertThat(payload.path("eventName").asText()).isEqualTo("backend_new_comment_count_view");
            assertThat(payload.path("resultCount")).isEqualTo(IntNode.valueOf(3));
        } finally {
            appender.stop();
        }
    }

    @Test
    void keyValuesCannotOverwriteAppenderFields() {
        StructuredLogtailAppender appender = newAppender();
        try (LogCapture capture = new LogCapture(LOGGER_NAME)) {
            LoggerFactory.getLogger(LOGGER_NAME).atInfo()
                    .addKeyValue("message", "다른 메시지")
                    .log("원래 메시지");

            assertThat(appender.buildPostData(capture.events().getFirst()))
                    .containsEntry("message", "원래 메시지");
        } finally {
            appender.stop();
        }
    }

    private StructuredLogtailAppender newAppender() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern("%msg%nopex");
        encoder.start();

        StructuredLogtailAppender appender = new StructuredLogtailAppender();
        appender.setEncoder(encoder);
        return appender;
    }
}
