package yeobaek.backend.notification.registration.domain;

import java.util.List;
import java.util.Map;
import yeobaek.backend.notification.domain.NotificationType;

public final class NotificationPreferences {
    private final List<NotificationPreference> values;

    public NotificationPreferences(List<NotificationPreference> values) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("알림 설정은 비어 있을 수 없습니다.");
        }
        if (values.stream().anyMatch(preference ->
                !preference.belongsToRegistration(values.getFirst().getRegistration()))) {
            throw new IllegalArgumentException("지정한 기기 등록에 속한 알림 설정만 관리할 수 있습니다.");
        }
        this.values = List.copyOf(values);
    }

    public void update(Map<NotificationType, Boolean> typeChanges) {
        typeChanges.forEach((type, enabled) -> preferenceOf(type).changeEnabled(enabled));
    }

    public boolean isTypeEnabled(NotificationType type) {
        return preferenceOf(type).isEnabled();
    }

    private NotificationPreference preferenceOf(NotificationType type) {
        return values.stream().filter(preference -> preference.hasNotificationType(type)).findFirst()
                .orElseThrow(() -> new IllegalStateException("등록 정보에 알림 유형별 설정이 없습니다: " + type));
    }
}
