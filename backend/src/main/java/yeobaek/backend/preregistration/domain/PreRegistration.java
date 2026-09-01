package yeobaek.backend.preregistration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Pattern;
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

    private static final int MAX_EMAIL_LENGTH = 254;
    private static final String LOCAL_PART = "[A-Z0-9!#$%&'*+/=?^_`{|}~-]+";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^" + LOCAL_PART + "(?:\\." + LOCAL_PART + ")*@[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?"
                    + "(?:\\.[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?)+$",
            Pattern.CASE_INSENSITIVE);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = MAX_EMAIL_LENGTH)
    private String email;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public PreRegistration(String email) {
        String normalizedEmail = normalize(email);
        validate(normalizedEmail);
        this.email = normalizedEmail;
        this.createdAt = LocalDateTime.now();
    }

    private static String normalize(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static void validate(String email) {
        if (email == null || email.isBlank() || email.length() > MAX_EMAIL_LENGTH
                || email.indexOf('@') > 64 || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("올바른 이메일 주소를 입력해 주세요.");
        }
    }
}
