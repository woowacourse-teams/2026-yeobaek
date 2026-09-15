package yeobaek.backend.preregistration.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "pre_registrations",
        uniqueConstraints = @UniqueConstraint(name = "uk_pre_registrations_email", columnNames = "email"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PreRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "email", nullable = false, length = Email.MAX_LENGTH))
    private Email email;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public PreRegistration(String email) {
        this.email = new Email(email);
        this.createdAt = LocalDateTime.now();
    }

    public String getEmail() {
        return email.value();
    }
}
