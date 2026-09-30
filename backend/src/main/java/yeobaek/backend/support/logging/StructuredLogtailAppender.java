package yeobaek.backend.support.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.logtail.logback.LogtailAppender;
import java.util.Map;
import org.slf4j.event.KeyValuePair;

public final class StructuredLogtailAppender extends LogtailAppender {

    @Override
    protected Map<String, Object> buildPostData(ILoggingEvent event) {
        Map<String, Object> payload = super.buildPostData(event);
        if (event.getKeyValuePairs() == null) {
            return payload;
        }
        for (KeyValuePair pair : event.getKeyValuePairs()) {
            if (!payload.containsKey(pair.key)) {
                payload.put(pair.key, pair.value);
            }
        }
        return payload;
    }
}
