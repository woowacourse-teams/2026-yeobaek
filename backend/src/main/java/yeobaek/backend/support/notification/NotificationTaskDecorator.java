package yeobaek.backend.support.notification;

import io.opentelemetry.context.Context;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

public class NotificationTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> submittingMdc = MDC.getCopyOfContextMap();
        Context submittingContext = Context.current();
        return submittingContext.wrap(() -> {
            Map<String, String> previousMdc = MDC.getCopyOfContextMap();
            try {
                restoreMdc(submittingMdc);
                runnable.run();
            } finally {
                restoreMdc(previousMdc);
            }
        });
    }

    private void restoreMdc(Map<String, String> context) {
        if (context == null) {
            MDC.clear();
        } else {
            MDC.setContextMap(context);
        }
    }
}
