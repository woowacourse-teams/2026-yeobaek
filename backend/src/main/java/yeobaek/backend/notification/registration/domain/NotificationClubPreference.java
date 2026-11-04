package yeobaek.backend.notification.registration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification_club_preferences", uniqueConstraints =
        @UniqueConstraint(name = "uk_notification_club_preferences_registration_club",
                columnNames = {"registration_id", "club_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationClubPreference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "registration_id", nullable = false)
    private Long registrationId;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private Instant updatedAt;

    public NotificationClubPreference(Long registrationId, Long clubId, boolean enabled) {
        this.registrationId = registrationId;
        this.clubId = clubId;
        this.enabled = enabled;
        this.updatedAt = Instant.now();
    }
}
