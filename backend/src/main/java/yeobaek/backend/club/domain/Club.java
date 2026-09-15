package yeobaek.backend.club.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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
import yeobaek.backend.book.domain.Sentence;

@Entity
@Table(name = "clubs", uniqueConstraints = {
        @UniqueConstraint(name = "uk_clubs_join_code", columnNames = "join_code")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Club {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "name", nullable = false, length = ClubName.MAX_LENGTH))
    private ClubName name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", updatable = false)
    private Book book;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "join_code", nullable = false, length = 10))
    private JoinCode joinCode;

    public Club(String name, Book book, JoinCode joinCode) {
        this.name = new ClubName(name);
        this.book = book;
        this.joinCode = joinCode;
    }

    public String getName() {
        return name.value();
    }

    public String getJoinCode() {
        return joinCode.value();
    }

    public boolean isReading(Passage passage) {
        return passage.belongsTo(book);
    }

    public boolean isReading(Sentence sentence) {
        return sentence.belongsTo(book);
    }

    public int totalPassageCount() {
        return book.getPassageCount();
    }

    public void ensureBookAvailable() {
        book.ensureAvailable();
    }
}
