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
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.domain.Sentence;
import yeobaek.backend.support.BadRequestException;
import yeobaek.backend.support.ErrorCode;

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

    public void ensureCanRead(Passage passage) {
        if (!isReading(passage)) {
            throw new BadRequestException(
                    ErrorCode.INVALID_REQUEST,
                    "해당 공개방의 도서에 속한 문단이 아닙니다.",
                    readingLogContext(passage));
        }
        ensureBookAvailable();
    }

    private Map<String, String> readingLogContext(Passage passage) {
        Map<String, String> context = new HashMap<>();
        if (id != null) {
            context.put("publicRoomId", id.toString());
        }
        if (passage.getId() != null) {
            context.put("passageId", passage.getId().toString());
        }
        return context;
    }
}
