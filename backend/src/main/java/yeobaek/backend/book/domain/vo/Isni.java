package yeobaek.backend.book.domain.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.regex.Pattern;

@Embeddable
public record Isni(@Column(name = "value", length = LENGTH) String value) {

    public static final int LENGTH = 16;
    private static final Pattern FORMAT = Pattern.compile("\\d{15}[\\dX]");

    public Isni {
        value = normalize(value);
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("ISNI는 공백·하이픈을 제외하고 16자리(끝자리 X 허용)여야 합니다.");
        }
    }

    static String normalize(String rawIsni) {
        if (rawIsni == null) {
            return null;
        }
        return rawIsni.replace(" ", "").replace("-", "");
    }
}
