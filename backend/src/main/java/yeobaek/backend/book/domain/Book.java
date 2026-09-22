package yeobaek.backend.book.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import yeobaek.backend.book.domain.vo.BookDeduplicationKey;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.book.domain.vo.PassageCount;
import yeobaek.backend.book.domain.vo.Publisher;
import yeobaek.backend.support.BadRequestException;
import yeobaek.backend.support.ErrorCode;

@Entity
@Table(name = "books")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Book {

    private static final int MAX_COVER_IMAGE_KEY_LENGTH = 80;
    private static final Pattern COVER_IMAGE_KEY_PATTERN = Pattern.compile(
            "^[^/]+(?:/[^/]+)*/book-covers/"
                    + "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp)$");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "title", nullable = false, length = BookTitle.MAX_LENGTH))
    private BookTitle title;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "publisher", length = Publisher.MAX_LENGTH))
    private Publisher publisher;

    private Integer publishedYear;

    @Column(length = MAX_COVER_IMAGE_KEY_LENGTH)
    private String coverImageKey;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "passage_count", nullable = false))
    private PassageCount passageCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @ColumnDefault("'ACTIVE'")
    private BookStatus status = BookStatus.ACTIVE;

    public Book(BookTitle title, Publisher publisher, Integer publishedYear, int passageCount, String coverImageKey) {
        validateCoverImageKey(coverImageKey);
        if (title == null) {
            throw new IllegalArgumentException("도서 제목은 필수입니다.");
        }
        this.title = title;
        this.publisher = publisher;
        this.publishedYear = publishedYear;
        this.passageCount = new PassageCount(passageCount);
        this.coverImageKey = coverImageKey;
    }

    public boolean isSame(Book other) {
        return Objects.equals(id, other.getId());
    }

    public BookDeduplicationKey deduplicationKey(Set<Long> authorIds) {
        return new BookDeduplicationKey(title, publisher, publishedYear, authorIds);
    }

    public void delete() {
        ensureAvailable();
        this.status = BookStatus.DELETED;
    }

    public void ensureAvailable() {
        if (status != BookStatus.ACTIVE) {
            throw new BadRequestException(
                    ErrorCode.BOOK_NOT_AVAILABLE,
                    "더 이상 이용할 수 없는 도서입니다.");
        }
    }

    public void replaceCoverImage(String coverImageKey) {
        if (coverImageKey == null) {
            throw new IllegalArgumentException("교체할 표지 이미지 키는 필수입니다.");
        }
        updateCoverImage(coverImageKey);
    }

    public void removeCoverImage() {
        updateCoverImage(null);
    }

    private void updateCoverImage(String coverImageKey) {
        ensureAvailable();
        validateCoverImageKey(coverImageKey);
        this.coverImageKey = coverImageKey;
    }

    public BookTitle getTitle() {
        return title;
    }

    public Publisher getPublisher() {
        return publisher;
    }

    public PassageCount getPassageCount() {
        return passageCount;
    }

    private void validateCoverImageKey(String coverImageKey) {
        if (coverImageKey != null
                && (coverImageKey.length() > MAX_COVER_IMAGE_KEY_LENGTH
                || !COVER_IMAGE_KEY_PATTERN.matcher(coverImageKey).matches())) {
            throw new IllegalArgumentException("유효하지 않은 표지 이미지 키입니다.");
        }
    }
}
