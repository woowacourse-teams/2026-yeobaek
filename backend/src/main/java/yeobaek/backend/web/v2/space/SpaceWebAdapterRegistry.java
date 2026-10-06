package yeobaek.backend.web.v2.space;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import yeobaek.backend.application.space.query.UnsupportedSpaceQueryException;
import yeobaek.backend.space.api.SpaceKind;

@Component
public final class SpaceWebAdapterRegistry {

    private final Map<SpaceKind, SpaceWebAdapter> adapters;

    public SpaceWebAdapterRegistry(List<SpaceWebAdapter> adapters) {
        Map<SpaceKind, SpaceWebAdapter> registered = new HashMap<>();
        for (SpaceWebAdapter adapter : adapters) {
            if (registered.putIfAbsent(adapter.kind(), adapter) != null) {
                throw new IllegalStateException("같은 공간 종류의 v2 웹 어댑터가 중복 등록되었습니다: " + adapter.kind());
            }
        }
        this.adapters = Map.copyOf(registered);
    }

    public SpaceWebAdapter get(SpaceKind kind) {
        SpaceWebAdapter adapter = adapters.get(kind);
        if (adapter == null) {
            throw new UnsupportedSpaceQueryException(kind, "지원하지 않는 공간 종류입니다: " + kind.value());
        }
        return adapter;
    }
}
