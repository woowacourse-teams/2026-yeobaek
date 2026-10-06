package yeobaek.backend.application.reading;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.space.api.SpaceKind;

@Component
public class ReadingProgressHandlerRegistry {

    private final Map<SpaceKind, ReadingProgressHandler> handlers;

    public ReadingProgressHandlerRegistry(List<ReadingProgressHandler> handlers) {
        Map<SpaceKind, ReadingProgressHandler> indexed = new HashMap<>();
        handlers.forEach(handler -> {
            if (indexed.putIfAbsent(handler.supportedKind(), handler) != null) {
                throw new IllegalStateException("독서 진도 구현이 중복 등록되었습니다: " + handler.supportedKind());
            }
        });
        this.handlers = Map.copyOf(indexed);
    }

    public ReadingProgressHandler get(SpaceKind kind) {
        ReadingProgressHandler handler = handlers.get(kind);
        if (handler == null) {
            throw new ReadingProgressPolicyException(ErrorCode.UNSUPPORTED_KIND,
                    "진도 기록을 지원하지 않는 공간 종류입니다: " + kind,
                    Map.of("kind", kind.value(), "reason", "UNSUPPORTED_SPACE_KIND"));
        }
        return handler;
    }
}
