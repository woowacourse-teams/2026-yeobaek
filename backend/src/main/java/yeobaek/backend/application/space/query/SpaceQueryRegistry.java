package yeobaek.backend.application.space.query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import yeobaek.backend.space.api.SpaceKind;

@Component
public final class SpaceQueryRegistry {

    private final Map<SpaceKind, SpaceQueryProvider> providers;
    private final Map<SpaceKind, PublicSpaceListProvider> publicProviders;

    public SpaceQueryRegistry(List<SpaceQueryProvider> providers, List<PublicSpaceListProvider> publicProviders) {
        this.providers = index(providers);
        this.publicProviders = indexPublic(publicProviders);
        if (!this.publicProviders.containsKey(SpaceKind.PUBLIC_ROOM)) {
            throw new IllegalStateException("공개방 목록 조회 구현이 등록되지 않았습니다.");
        }
    }

    public SpaceQueryProvider get(SpaceKind kind) {
        SpaceQueryProvider provider = providers.get(kind);
        if (provider == null) {
            throw new UnsupportedSpaceQueryException(kind, "지원하지 않는 공간 조회 종류입니다: " + kind);
        }
        return provider;
    }

    public PublicSpaceListProvider getPublic(SpaceKind kind) {
        PublicSpaceListProvider provider = publicProviders.get(kind);
        if (provider == null) {
            throw new UnsupportedSpaceQueryException(kind, "공개 목록을 지원하지 않는 공간 종류입니다: " + kind);
        }
        return provider;
    }

    private Map<SpaceKind, SpaceQueryProvider> index(List<SpaceQueryProvider> candidates) {
        Map<SpaceKind, SpaceQueryProvider> indexed = new HashMap<>();
        candidates.forEach(provider -> put(indexed, provider.supportedKind(), provider));
        return Map.copyOf(indexed);
    }

    private Map<SpaceKind, PublicSpaceListProvider> indexPublic(List<PublicSpaceListProvider> candidates) {
        Map<SpaceKind, PublicSpaceListProvider> indexed = new HashMap<>();
        candidates.forEach(provider -> put(indexed, provider.supportedKind(), provider));
        return Map.copyOf(indexed);
    }

    private <T> void put(Map<SpaceKind, T> indexed, SpaceKind kind, T provider) {
        if (indexed.putIfAbsent(kind, provider) != null) {
            throw new IllegalStateException("공간 조회 구현이 중복 등록되었습니다: " + kind);
        }
    }
}
