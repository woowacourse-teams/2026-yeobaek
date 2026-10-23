package yeobaek.backend.support.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class NotificationTaskDecoratorTest {

    private static final ContextKey<String> REQUEST_KEY = ContextKey.named("notification-test-request");
    private final NotificationTaskDecorator decorator = new NotificationTaskDecorator();
    private Map<String, String> previousMdc;

    @BeforeEach
    void setUp() {
        previousMdc = MDC.getCopyOfContextMap();
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        if (previousMdc == null) {
            MDC.clear();
        } else {
            MDC.setContextMap(previousMdc);
        }
    }

    @Test
    @DisplayName("제출 시점의 MDC·OpenTelemetry 컨텍스트를 전달하고 워커의 이전 상태를 복원한다")
    void propagateAndRestoreContext() throws Exception {
        var captured = new AtomicReference<List<String>>();
        MDC.put("memberId", "7");
        Callable<Runnable> prepareTask = () -> decorator.decorate(() ->
                captured.set(List.of(MDC.get("memberId"), Context.current().get(REQUEST_KEY))));
        Runnable task = Context.current().with(REQUEST_KEY, "request").wrap(prepareTask).call();
        MDC.put("memberId", "worker-previous");
        Context.current().with(REQUEST_KEY, "worker").wrap(() -> {
            task.run();

            assertThat(captured.get()).containsExactly("7", "request");
            assertThat(MDC.get("memberId")).isEqualTo("worker-previous");
            assertThat(Context.current().get(REQUEST_KEY)).isEqualTo("worker");
        }).run();
    }

    @Test
    @DisplayName("요청 MDC가 없으면 이전 값을 작업에 노출하지 않고 예외 후에도 컨텍스트를 복원한다")
    void clearAbsentContextAndRestoreAfterFailure() {
        Runnable task = decorator.decorate(() -> {
            assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
            assertThat(Context.current().get(REQUEST_KEY)).isNull();
            throw new IllegalStateException("failed");
        });
        MDC.put("memberId", "worker-previous");
        Context.current().with(REQUEST_KEY, "worker").wrap(() -> {
            assertThatThrownBy(task::run).isInstanceOf(IllegalStateException.class);

            assertThat(MDC.get("memberId")).isEqualTo("worker-previous");
            assertThat(Context.current().get(REQUEST_KEY)).isEqualTo("worker");
        }).run();
    }
}
