package yeobaek.backend.preregistration.domain.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Locale;
import java.util.regex.Pattern;

@Embeddable
public record Email(@Column(name = "value", nullable = false, length = MAX_LENGTH) String value) {

    public static final int MAX_LENGTH = 254;
    private static final String LOCAL_PART = "[A-Z0-9!#$%&'*+/=?^_`{|}~-]+";
    private static final Pattern FORMAT = Pattern.compile(
            "^" + LOCAL_PART + "(?:\\." + LOCAL_PART + ")*@[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?"
                    + "(?:\\.[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?)+$",
            Pattern.CASE_INSENSITIVE);

    public Email {
        value = normalize(value);
        if (value == null || value.isBlank() || value.length() > MAX_LENGTH
                || value.indexOf('@') > 64 || !FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("올바른 이메일 주소를 입력해 주세요.");
        }
    }

    private static String normalize(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
