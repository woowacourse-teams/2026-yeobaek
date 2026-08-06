package watson.backend.book.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "authors")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Author {

    private static final int MAX_NAME_LENGTH = 100;
    private static final Pattern ISNI_PATTERN = Pattern.compile("\\d{15}[\\dX]");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    @Column(unique = true, length = 16)
    private String isni;

    public Author(String name) {
        this(name, null);
    }

    public Author(String name, String isni) {
        validateName(name);
        this.name = name;
        this.isni = normalizeIsni(isni);
    }

    /**
     * 공백·하이픈을 제거한 16자리(끝자리 X 허용) 형식으로 정규화한다. 체크섬은 검증하지 않는다 (API.md 6장).
     */
    public static String normalizeIsni(String rawIsni) {
        if (rawIsni == null) {
            return null;
        }
        String normalized = rawIsni.replace(" ", "").replace("-", "");
        if (!ISNI_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("ISNI는 공백·하이픈을 제외하고 16자리(끝자리 X 허용)여야 합니다.");
        }
        return normalized;
    }

    public boolean hasSameName(String otherName) {
        return name.equals(otherName);
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank() || name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("작가 이름은 공백이 아닌 1~" + MAX_NAME_LENGTH + "자여야 합니다.");
        }
    }
}
