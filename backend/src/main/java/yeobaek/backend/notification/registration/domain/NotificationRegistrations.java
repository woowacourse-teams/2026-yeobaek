package yeobaek.backend.notification.registration.domain;

import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class NotificationRegistrations {
    private final Collection<NotificationRegistration> values;

    public List<Long> idsFor(FirebaseInstallationId fid) {
        return values.stream()
                .filter(registration -> registration.isFor(fid))
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
