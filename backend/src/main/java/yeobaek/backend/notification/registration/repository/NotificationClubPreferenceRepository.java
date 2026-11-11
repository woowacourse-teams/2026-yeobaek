package yeobaek.backend.notification.registration.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import yeobaek.backend.notification.registration.domain.NotificationClubPreference;

public interface NotificationClubPreferenceRepository extends JpaRepository<NotificationClubPreference, Long> {
    Optional<NotificationClubPreference> findByRegistrationIdAndClubId(Long registrationId, Long clubId);
}
