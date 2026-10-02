package yeobaek.backend.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CapabilityRegistryTest {

    @Test
    @DisplayName("새 타입 capability는 중앙 enum이나 switch 변경 없이 등록된다")
    void registerThirdProvider() {
        var registry = new CapabilityRegistry<>(
                List.of(new TestCapability("CLUB"), new TestCapability("PUBLIC_ROOM"),
                        new TestCapability("THIRD_SPACE")),
                TestCapability::kind);

        assertThat(registry.get("THIRD_SPACE").kind()).isEqualTo("THIRD_SPACE");
    }

    @Test
    @DisplayName("같은 타입 capability 중복 등록은 조립 시점에 거부된다")
    void rejectDuplicateProvider() {
        var duplicates = List.of(new TestCapability("CLUB"), new TestCapability("CLUB"));

        assertThatThrownBy(() -> new CapabilityRegistry<>(duplicates, TestCapability::kind))
                .isInstanceOf(IllegalStateException.class);
    }

    private record TestCapability(String kind) {
    }
}
