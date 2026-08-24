package yeobaek.backend.book.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import yeobaek.backend.support.BadRequestException;
import yeobaek.backend.support.ErrorCode;

@Entity
@Table(name = "books")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Book {

    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MAX_PUBLISHER_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = MAX_TITLE_LENGTH)
    private String title;

    @Column(length = MAX_PUBLISHER_LENGTH)
    private String publisher;

    private Integer publishedYear;

    @Column(nullable = false)
    private int passageCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @ColumnDefault("'ACTIVE'")
    private BookStatus status = BookStatus.ACTIVE;

    public Book(String title, String publisher, Integer publishedYear, int passageCount) {
        validateTitle(title);
        validatePublisher(publisher);
        this.title = title;
        this.publisher = publisher;
        this.publishedYear = publishedYear;
        this.passageCount = passageCount;
    }

    public boolean isSame(Book other) {
        return Objects.equals(id, other.getId());
    }

    public boolean hasSameBibliography(Book other) {
        return title.equals(other.getTitle())
                && Objects.equals(publisher, other.getPublisher())
                && Objects.equals(publishedYear, other.getPublishedYear());
    }

    public void delete() {
        ensureAvailable();
        this.status = BookStatus.DELETED;
    }

    public void ensureAvailable() {
        if (status != BookStatus.ACTIVE) {
            throw new BadRequestException(ErrorCode.BOOK_NOT_AVAILABLE);
        }
    }

    private static void validateTitle(String title) {
        if (title == null || title.isBlank() || title.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("도서 제목은 공백이 아닌 1~" + MAX_TITLE_LENGTH + "자여야 합니다.");
        }
    }

    private static void validatePublisher(String publisher) {
        if (publisher != null && (publisher.isBlank() || publisher.length() > MAX_PUBLISHER_LENGTH)) {
            throw new IllegalArgumentException("출판사는 공백이 아닌 1~" + MAX_PUBLISHER_LENGTH + "자여야 합니다.");
        }
    }
}
