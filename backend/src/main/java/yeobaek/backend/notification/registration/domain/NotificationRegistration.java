package yeobaek.backend.notification.registration.domain;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import yeobaek.backend.member.domain.Member;

@Entity
@Table(name = "notification_registrations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class NotificationRegistration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Embedded
    private FirebaseInstallationId firebaseInstallationId;

    public NotificationRegistration(Member member, FirebaseInstallationId firebaseInstallationId) {
        this.member = Objects.requireNonNull(member);
        this.firebaseInstallationId = Objects.requireNonNull(firebaseInstallationId);
    }

    public boolean isFor(FirebaseInstallationId fid) {
        return firebaseInstallationId.equals(fid);
    }
}
