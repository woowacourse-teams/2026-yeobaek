package yeobaek.backend.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.opentelemetry.autoconfigure.OpenTelemetryProperties;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.io.support.ResourcePropertySource;

class ObservabilityPropertiesTest {

    @Test
    void composeEnvironmentBindsAllSignalsAndResource() throws IOException {
        var environment = environment(Map.of(
                "OBSERVABILITY_ENABLED", "true",
                "OBSERVABILITY_OTLP_ENDPOINT", "http://otel-collector:4318",
                "DEPLOYMENT_ENVIRONMENT", "dev",
                "SPRING_APPLICATION_NAME", "yeobaek-api",
                "MANAGEMENT_TRACING_SAMPLING_PROBABILITY", "1.0"));

        assertThat(environment.getProperty("management.logging.export.otlp.enabled", Boolean.class)).isTrue();
        assertThat(environment.getProperty("management.tracing.export.otlp.enabled", Boolean.class)).isTrue();
        assertThat(environment.getProperty("management.otlp.metrics.export.enabled", Boolean.class)).isTrue();
        assertThat(Binder.get(environment).bind("management.opentelemetry", OpenTelemetryProperties.class)
                .get().getResourceAttributes()).containsEntry("deployment.environment.name", "dev");
        assertThat(Binder.get(environment).bind("management.tracing.sampling.probability", Double.class).get())
                .isEqualTo(1.0);
    }

    @Test
    void endpointHasSignalSpecificPaths() throws IOException {
        var environment = environment(Map.of("OBSERVABILITY_OTLP_ENDPOINT", "http://otel-collector:4318"));

        assertThat(environment.getProperty("management.opentelemetry.logging.export.otlp.endpoint"))
                .isEqualTo("http://otel-collector:4318/v1/logs");
        assertThat(environment.getProperty("management.opentelemetry.tracing.export.otlp.endpoint"))
                .isEqualTo("http://otel-collector:4318/v1/traces");
        assertThat(environment.getProperty("management.otlp.metrics.export.url"))
                .isEqualTo("http://otel-collector:4318/v1/metrics");
    }

    @Test
    void disableExportAndRestrictManagementByDefault() throws IOException {
        var environment = environment(Map.of());

        assertThat(environment.getProperty("management.logging.export.otlp.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("management.tracing.export.otlp.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("management.otlp.metrics.export.enabled", Boolean.class)).isFalse();
        assertThat(InetAddress.getByName(environment.getProperty("management.server.address")).isLoopbackAddress())
                .isTrue();
        assertThat(environment.getProperty("management.server.port", Integer.class)).isEqualTo(8081);
    }

    private StandardEnvironment environment(Map<String, Object> variables) throws IOException {
        var environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new ResourcePropertySource("classpath:observability.properties"));
        environment.getPropertySources().addFirst(new SystemEnvironmentPropertySource("compose", variables));
        return environment;
    }
}
