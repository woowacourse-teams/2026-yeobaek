package yeobaek.backend.book.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "authors")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "name", nullable = false, length = AuthorName.MAX_LENGTH))
    private AuthorName name;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "isni", unique = true, length = Isni.LENGTH))
    private Isni isni;

    public Author(String name) {
        this(name, null);
    }

    public Author(String name, String isni) {
        this.name = new AuthorName(name);
        if (isni != null) {
            this.isni = new Isni(isni);
        }
    }

    /**
     * 공백·하이픈을 제거한 16자리(끝자리 X 허용) 형식으로 정규화한다. 체크섬은 검증하지 않는다 (API.md 6장).
     */
    public static String normalizeIsni(String rawIsni) {
        return rawIsni == null ? null : new Isni(rawIsni).value();
    }

    public boolean hasSameName(String otherName) {
        return name.value().equals(otherName);
    }

    public String getName() {
        return name.value();
    }

    public String getIsni() {
        return isni == null ? null : isni.value();
    }
}
