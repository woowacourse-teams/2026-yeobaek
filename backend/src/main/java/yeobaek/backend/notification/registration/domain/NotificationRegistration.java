package yeobaek.backend.notification.registration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import yeobaek.backend.member.domain.Member;

@Entity
@Table(name = "notification_registrations", uniqueConstraints =
        @UniqueConstraint(name = "uk_notification_registrations_member_fid", columnNames = {"member_id", "fid"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationRegistration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Embedded
    private FirebaseInstallationId firebaseInstallationId;

    @Column(nullable = false)
    private boolean enabled = true;

    public NotificationRegistration(Member member, FirebaseInstallationId firebaseInstallationId) {
        this(null, member, firebaseInstallationId);
    }

    public NotificationRegistration(Long id, Member member, FirebaseInstallationId firebaseInstallationId) {
        this.id = id;
        this.member = Objects.requireNonNull(member);
        this.firebaseInstallationId = Objects.requireNonNull(firebaseInstallationId);
    }

    public void changeEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean hasInstallationId(FirebaseInstallationId fid) {
        return firebaseInstallationId.equals(fid);
    }
}
