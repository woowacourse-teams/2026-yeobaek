package yeobaek.backend.notification.registration.repository;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yeobaek.backend.notification.registration.domain.NotificationClubPreference;

public interface NotificationClubPreferenceRepository extends JpaRepository<NotificationClubPreference, Long> {
    Optional<NotificationClubPreference> findByRegistrationIdAndClubId(Long registrationId, Long clubId);

    @Modifying
    @Query(value = """
            insert into notification_club_preferences (registration_id, club_id, enabled, updated_at)
            values (:registrationId, :clubId, :enabled, :updatedAt)
            on duplicate key update enabled = :enabled, updated_at = :updatedAt
            """, nativeQuery = true)
    int upsert(@Param("registrationId") Long registrationId, @Param("clubId") Long clubId,
               @Param("enabled") boolean enabled, @Param("updatedAt") Instant updatedAt);
}
