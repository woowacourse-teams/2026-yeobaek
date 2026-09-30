package yeobaek.backend.publicroom.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.domain.Sentence;

@Entity
@Table(name = "public_rooms", uniqueConstraints = {
        @UniqueConstraint(name = "uk_public_rooms_book", columnNames = "book_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PublicRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false, updatable = false)
    private Book book;

    public PublicRoom(Book book) {
        this.book = book;
    }

    public boolean isReading(Passage passage) {
        return passage.belongsTo(book);
    }

    public boolean isReading(Sentence sentence) {
        return sentence.belongsTo(book);
    }

    public void ensureBookAvailable() {
        book.ensureAvailable();
    }
}
