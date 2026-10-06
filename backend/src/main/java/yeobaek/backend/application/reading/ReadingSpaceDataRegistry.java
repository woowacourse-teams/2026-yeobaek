package yeobaek.backend.application.reading;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import yeobaek.backend.space.api.SpaceKind;

@Component
public class ReadingSpaceDataRegistry {

    private final Map<SpaceKind, ReadingSpaceDataProvider> providers;

    public ReadingSpaceDataRegistry(List<ReadingSpaceDataProvider> providers) {
        Map<SpaceKind, ReadingSpaceDataProvider> indexed = new HashMap<>();
        providers.forEach(provider -> {
            if (indexed.putIfAbsent(provider.supportedKind(), provider) != null) {
                throw new IllegalStateException("읽기 공간 응답 구현이 중복 등록되었습니다: " + provider.supportedKind());
            }
        });
        this.providers = Map.copyOf(indexed);
    }

    public ReadingSpaceDataProvider get(SpaceKind kind) {
        ReadingSpaceDataProvider provider = providers.get(kind);
        if (provider == null) {
            throw new IllegalArgumentException("지원하지 않는 읽기 공간 종류입니다: " + kind);
        }
        return provider;
    }
}
