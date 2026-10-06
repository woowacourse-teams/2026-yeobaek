package yeobaek.backend.web.v2.appreciation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import yeobaek.backend.appreciation.api.AppreciationKind;

@Component
public final class AppreciationWebAdapterRegistry {

    private final Map<AppreciationKind, AppreciationWebAdapter> adapters;

    public AppreciationWebAdapterRegistry(List<AppreciationWebAdapter> adapters) {
        Map<AppreciationKind, AppreciationWebAdapter> registered = new HashMap<>();
        for (AppreciationWebAdapter adapter : adapters) {
            if (registered.putIfAbsent(adapter.kind(), adapter) != null) {
                throw new IllegalStateException("같은 감상 종류의 v2 웹 어댑터가 중복 등록되었습니다: " + adapter.kind());
            }
        }
        this.adapters = Map.copyOf(registered);
    }

    public AppreciationWebAdapter get(AppreciationKind kind) {
        AppreciationWebAdapter adapter = adapters.get(kind);
        if (adapter == null) {
            throw new IllegalArgumentException("지원하지 않는 감상 종류입니다: " + kind.value());
        }
        return adapter;
    }
}
