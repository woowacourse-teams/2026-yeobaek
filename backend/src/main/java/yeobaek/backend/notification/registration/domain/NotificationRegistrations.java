package yeobaek.backend.notification.registration.domain;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class NotificationRegistrations {
    private final Collection<NotificationRegistration> values;

    public Set<String> firebaseInstallationIds() {
        return values.stream()
                .map(NotificationRegistration::getFirebaseInstallationId)
                .map(FirebaseInstallationId::value)
                .collect(Collectors.toUnmodifiableSet());
    }
}
