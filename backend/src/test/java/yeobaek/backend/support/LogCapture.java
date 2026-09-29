package yeobaek.backend.support;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import org.slf4j.LoggerFactory;

/** Captures MDC while the request still owns the thread, and always detaches the appender. */
public final class LogCapture extends ListAppender<ILoggingEvent> implements AutoCloseable {

    private final Logger logger;
    private final Level previousLevel;

    public LogCapture(String loggerName) {
        logger = (Logger) LoggerFactory.getLogger(loggerName);
        previousLevel = logger.getLevel();
        logger.setLevel(Level.INFO);
        start();
        logger.addAppender(this);
    }

    @Override
    protected void append(ILoggingEvent event) {
        event.prepareForDeferredProcessing();
        super.append(event);
    }

    public List<ILoggingEvent> events() {
        return List.copyOf(list);
    }

    public Object field(ILoggingEvent event, String key) {
        if (event.getKeyValuePairs() == null) {
            return null;
        }
        for (var pair : event.getKeyValuePairs()) {
            if (pair.key.equals(key)) {
                return pair.value;
            }
        }
        return null;
    }

    public boolean hasField(ILoggingEvent event, String key) {
        return event.getKeyValuePairs() != null
                && event.getKeyValuePairs().stream().anyMatch(pair -> pair.key.equals(key));
    }

    public String structuredText() {
        return list.stream().map(event -> event.getFormattedMessage() + event.getKeyValuePairs()
                        + event.getMDCPropertyMap())
                .reduce("", (left, right) -> left + "\n" + right);
    }

    @Override
    public void close() {
        logger.detachAppender(this);
        logger.setLevel(previousLevel);
        stop();
    }
}
