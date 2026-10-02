package yeobaek.backend.bootstrap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class CapabilityRegistry<K, T> {

    private final Map<K, T> capabilities;

    public CapabilityRegistry(List<T> candidates, Function<T, K> keyExtractor) {
        Map<K, T> registered = new HashMap<>();
        for (T capability : candidates) {
            K key = keyExtractor.apply(capability);
            T duplicate = registered.putIfAbsent(key, capability);
            if (duplicate != null) {
                throw new IllegalStateException("같은 타입의 capability를 중복 등록할 수 없습니다: " + key);
            }
        }
        capabilities = Map.copyOf(registered);
    }

    public T get(K key) {
        T capability = capabilities.get(key);
        if (capability == null) {
            throw new IllegalArgumentException("지원하지 않는 capability입니다: " + key);
        }
        return capability;
    }
}
