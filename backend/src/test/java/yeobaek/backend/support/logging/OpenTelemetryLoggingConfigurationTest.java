package yeobaek.backend.support.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

class OpenTelemetryLoggingConfigurationTest {

    @Test
    void installSpringSdkReplayStartupAndCaptureActiveSpan() throws Exception {
        List<LogRecordData> captured = new ArrayList<>();
        try (LogRecordExporter exporter = mock(LogRecordExporter.class)) {
            when(exporter.export(anyCollection())).thenAnswer(invocation -> {
                captured.addAll(invocation.getArgument(0));
                return CompletableResultCode.ofSuccess();
            });
            when(exporter.shutdown()).thenReturn(CompletableResultCode.ofSuccess());
            try (var loggerProvider = SdkLoggerProvider.builder()
                    .addLogRecordProcessor(SimpleLogRecordProcessor.create(exporter)).build();
                 var sdk = OpenTelemetrySdk.builder().setLoggerProvider(loggerProvider)
                    .setTracerProvider(SdkTracerProvider.builder().build()).build()) {
                OpenTelemetryAppender appender = new OpenTelemetryAppender();
                var context = (LoggerContext) LoggerFactory.getILoggerFactory();
                appender.setContext(context);
                appender.setCaptureKeyValuePairAttributes(true);
                appender.setCaptureMdcAttributes("memberId");
                appender.start();
                Logger logger = context.getLogger(getClass());
                logger.addAppender(appender);
                try {
                    logger.info("startup probe");
                    new OpenTelemetryLoggingConfiguration().openTelemetryLogbackInstaller(sdk).afterPropertiesSet();
                    var span = sdk.getTracer("probe").spanBuilder("active probe").startSpan();
                    var scope = span.makeCurrent();
                    try (scope) {
                        MDC.put("memberId", "7");
                        logger.atError().addKeyValue("operation", "probe").addKeyValue("result", "failure")
                                .addKeyValue("count", 3).addKeyValue("cached", false)
                                .setCause(new IllegalStateException("failure probe")).log("typed probe");
                    } finally {
                        span.end();
                        MDC.remove("memberId");
                    }
                    assertThat(captured).anySatisfy(log -> {
                        assertThat(log.getBodyValue().asString()).isEqualTo("startup probe");
                        assertThat(log.getSpanContext().isValid()).isFalse();
                    }).anySatisfy(log -> {
                        assertThat(log.getBodyValue().asString()).isEqualTo("typed probe");
                        assertThat(log.getSpanContext()).isEqualTo(span.getSpanContext());
                        assertThat(log.getAttributes().get(AttributeKey.longKey("count"))).isEqualTo(3L);
                        assertThat(log.getAttributes().get(AttributeKey.booleanKey("cached"))).isFalse();
                        assertThat(log.getAttributes().get(AttributeKey.stringKey("memberId"))).isEqualTo("7");
                    });
                    assertThat(captured).anySatisfy(log -> {
                        assertThat(log.getAttributes().get(AttributeKey.stringKey("exception.type")))
                                .isEqualTo(IllegalStateException.class.getName());
                        assertThat(log.getAttributes().get(AttributeKey.stringKey("exception.stacktrace")))
                                .contains("failure probe");
                    });
                } finally {
                    logger.detachAppender(appender);
                    appender.stop();
                }
            }
        }
    }
}
