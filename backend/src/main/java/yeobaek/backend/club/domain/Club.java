package yeobaek.backend.club.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.Passage;

@Entity
@Table(name = "clubs", uniqueConstraints = {
        @UniqueConstraint(name = "uk_clubs_join_code", columnNames = "join_code")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Club {

    private static final int MAX_NAME_LENGTH = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", updatable = false)
    private Book book;

    @Column(name = "join_code", nullable = false, length = 10)
    private String joinCode;

    public Club(String name, Book book, String joinCode) {
        validate(name);
        this.name = name;
        this.book = book;
        this.joinCode = joinCode;
    }

    private static void validate(String name) {
        if (name == null || name.isBlank() || name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("모임 이름은 공백이 아닌 1~" + MAX_NAME_LENGTH + "자여야 합니다.");
        }
    }

    public boolean isReading(Passage passage) {
        return passage.belongsTo(book);
    }

    public int totalPassageCount() {
        return book.getPassageCount();
    }
}
