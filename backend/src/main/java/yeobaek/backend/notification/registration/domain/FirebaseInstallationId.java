package yeobaek.backend.notification.registration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.regex.Pattern;

@Embeddable
public record FirebaseInstallationId(
        @Column(name = "fid", nullable = false) String value
) {
    private static final Pattern FORMAT =
            Pattern.compile("^[cdef][A-Za-z0-9_-]{21}$");

    public FirebaseInstallationId {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("FID 형식이 올바르지 않습니다.");
        }
    }
}
