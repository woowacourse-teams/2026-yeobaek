package yeobaek.backend.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CapabilityMapTest {

    @Test
    @DisplayName("타입별 capability를 등록 순서대로 선택할 수 있다")
    void selectsCapabilitiesInRegistrationOrder() {
        TestCapability club = new TestCapability("CLUB");
        TestCapability publicRoom = new TestCapability("PUBLIC_ROOM");
        TestCapability thirdSpace = new TestCapability("THIRD_SPACE");

        Map<String, TestCapability> capabilities = CapabilityMap.from(
                List.of(club, publicRoom, thirdSpace), TestCapability::kind);

        assertThat(capabilities.get("THIRD_SPACE")).isSameAs(thirdSpace);
        assertThat(capabilities.values()).containsExactly(club, publicRoom, thirdSpace);
        assertThat(capabilities.get("UNKNOWN")).isNull();
    }

    @Test
    @DisplayName("조립된 capability map은 변경할 수 없다")
    void returnsImmutableMap() {
        Map<String, TestCapability> capabilities = CapabilityMap.from(
                List.of(new TestCapability("CLUB")), TestCapability::kind);

        assertThatThrownBy(() -> capabilities.put("PUBLIC_ROOM", new TestCapability("PUBLIC_ROOM")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("같은 타입 capability 중복 등록은 조립 시점에 거부된다")
    void rejectsDuplicateCapability() {
        var duplicates = List.of(new TestCapability("CLUB"), new TestCapability("CLUB"));

        assertThatThrownBy(() -> CapabilityMap.from(duplicates, TestCapability::kind))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("타입이 null인 capability는 조립 시점에 거부된다")
    void rejectsNullCapabilityKey() {
        assertThatThrownBy(() -> CapabilityMap.from(List.of(new TestCapability(null)), TestCapability::kind))
                .isInstanceOf(NullPointerException.class);
    }

    private record TestCapability(String kind) {
    }
}
