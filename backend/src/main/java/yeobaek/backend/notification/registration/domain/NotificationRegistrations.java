package yeobaek.backend.notification.registration.domain;

import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import yeobaek.backend.notification.domain.NotificationType;

@RequiredArgsConstructor
public class NotificationRegistrations {
    private final Collection<NotificationRegistration> values;

    public static NotificationRegistrations fromEnabledPreferences(
            List<NotificationPreference> preferences, NotificationType type) {
        return new NotificationRegistrations(preferences.stream()
                .filter(preference -> preference.isNotificationAllowed(type))
                .map(NotificationPreference::getRegistration).toList());
    }

    public List<Long> findRegistrationIdsByFid(FirebaseInstallationId fid) {
        return values.stream()
                .filter(registration -> registration.hasInstallationId(fid))
                .map(NotificationRegistration::getId)
                .toList();
    }

    public List<String> firebaseInstallationIds() {
        return values.stream()
                .map(NotificationRegistration::getFirebaseInstallationId)
                .map(FirebaseInstallationId::value)
                .distinct()
                .toList();
    }
}
