package yeobaek.backend.bootstrap;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

final class CapabilityMap {

    private CapabilityMap() {
    }

    static <K, T> Map<K, T> from(List<T> candidates, Function<T, K> keyExtractor) {
        Map<K, T> registered = new LinkedHashMap<>();
        for (T capability : candidates) {
            K key = Objects.requireNonNull(keyExtractor.apply(capability));
            T duplicate = registered.putIfAbsent(key, capability);
            if (duplicate != null) {
                throw new IllegalStateException("같은 타입의 capability를 중복 등록할 수 없습니다: " + key);
            }
        }
        return Collections.unmodifiableMap(registered);
    }
}
