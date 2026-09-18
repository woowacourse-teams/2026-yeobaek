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
import yeobaek.backend.book.domain.vo.AuthorName;
import yeobaek.backend.book.domain.vo.Isni;

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

    public Author(String name, Isni isni) {
        this.name = new AuthorName(name);
        this.isni = isni;
    }

    public boolean hasSameName(AuthorName otherName) {
        return name.equals(otherName);
    }

    public AuthorName getName() {
        return name;
    }

    public Isni getIsni() {
        return isni;
    }
}
