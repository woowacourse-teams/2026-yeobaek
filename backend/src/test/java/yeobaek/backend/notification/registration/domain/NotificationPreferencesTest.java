package yeobaek.backend.notification.registration.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.notification.domain.NotificationType;

class NotificationPreferencesTest {
    @ParameterizedTest
    @EnumSource(NotificationType.class)
    @DisplayName("유형별 변경은 전달된 유형에만 적용하고 나머지는 유지해요")
    void updateOnlyRequestedType(NotificationType type) {
        var preferences = preferences();

        preferences.update(Map.of(type, false));

        assertThat(preferences.isTypeEnabled(type)).isFalse();
        assertThat(List.of(NotificationType.values()).stream().filter(value -> value != type))
                .allMatch(preferences::isTypeEnabled);
    }

    @Test
    @DisplayName("다른 기기의 설정을 하나의 컬렉션에 섞을 수 없어요")
    void rejectMixedRegistrations() {
        var registration = registration(1L);
        var values = new ArrayList<>(NotificationPreference.defaultsFor(registration));
        values.addAll(NotificationPreference.defaultsFor(registration(2L)));

        assertThatThrownBy(() -> new NotificationPreferences(values))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("빈 설정이나 없는 유형을 임의로 ON으로 처리하지 않아요")
    void rejectMissingSettings() {
        var registration = registration(1L);
        assertThatThrownBy(() -> new NotificationPreferences(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        var preferences = new NotificationPreferences(List.of(
                new NotificationPreference(registration, NotificationType.CLUB_COMMENT)));

        assertThatThrownBy(() -> preferences.update(Map.of(NotificationType.CLUB_JOIN, false)))
                .isInstanceOf(IllegalStateException.class);
    }

    private NotificationPreferences preferences() {
        var registration = registration(1L);
        return new NotificationPreferences(NotificationPreference.defaultsFor(registration));
    }

    private NotificationRegistration registration(Long id) {
        return new NotificationRegistration(id, mock(Member.class),
                new FirebaseInstallationId("c" + "a".repeat(21)));
    }
}
