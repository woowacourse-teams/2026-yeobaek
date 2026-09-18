package yeobaek.backend.club.domain.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.security.SecureRandom;
import java.util.regex.Pattern;

@Embeddable
public record JoinCode(@Column(name = "value", nullable = false, length = LENGTH) String value) {

    static final int LENGTH = 6;
    private static final String CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final Pattern FORMAT = Pattern.compile("[A-Z0-9]{" + LENGTH + "}");
    private static final SecureRandom RANDOM = new SecureRandom();

    public JoinCode {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("참여 코드는 " + LENGTH + "자 대문자와 숫자로 구성되어야 합니다.");
        }
    }

    public static JoinCode generate() {
        StringBuilder value = new StringBuilder(LENGTH);
        for (int index = 0; index < LENGTH; index++) {
            value.append(CHARSET.charAt(RANDOM.nextInt(CHARSET.length())));
        }
        return new JoinCode(value.toString());
    }
}
