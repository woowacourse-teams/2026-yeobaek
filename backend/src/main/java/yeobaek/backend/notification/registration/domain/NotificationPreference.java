package yeobaek.backend.notification.registration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import yeobaek.backend.notification.domain.NotificationType;

@Entity
@Table(name = "notification_preferences", uniqueConstraints =
        @UniqueConstraint(name = "uk_notification_preferences_registration_type",
                columnNames = {"registration_id", "notification_type"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationPreference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registration_id", nullable = false)
    private NotificationRegistration registration;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private Instant updatedAt;

    NotificationPreference(NotificationRegistration registration, NotificationType notificationType) {
        this.registration = registration;
        this.notificationType = notificationType;
        this.enabled = true;
        this.updatedAt = Instant.now();
    }

    public static List<NotificationPreference> defaultsFor(NotificationRegistration registration) {
        return Arrays.stream(NotificationType.values())
                .map(type -> new NotificationPreference(registration, type)).toList();
    }

    public boolean belongsToRegistration(NotificationRegistration registration) {
        return Objects.equals(this.registration, registration)
                || (this.registration.getId() != null
                && this.registration.getId().equals(registration.getId()));
    }

    public boolean hasNotificationType(NotificationType type) {
        return notificationType == type;
    }

    public boolean isNotificationAllowed(NotificationType type) {
        return hasNotificationType(type) && enabled && registration.isEnabled();
    }

    public void changeEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            this.updatedAt = Instant.now();
        }
    }
}
