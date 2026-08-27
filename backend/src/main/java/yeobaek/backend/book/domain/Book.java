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
import java.util.regex.Pattern;
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
    private static final int MAX_COVER_IMAGE_KEY_LENGTH = 80;
    private static final Pattern COVER_IMAGE_KEY_PATTERN = Pattern.compile(
            "^[^/]+(?:/[^/]+)*/book-covers/"
                    + "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp)$");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = MAX_TITLE_LENGTH)
    private String title;

    @Column(length = MAX_PUBLISHER_LENGTH)
    private String publisher;

    private Integer publishedYear;

    @Column(length = MAX_COVER_IMAGE_KEY_LENGTH)
    private String coverImageKey;

    @Column(nullable = false)
    private int passageCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @ColumnDefault("'ACTIVE'")
    private BookStatus status = BookStatus.ACTIVE;

    public Book(String title, String publisher, Integer publishedYear, int passageCount) {
        this(title, publisher, publishedYear, passageCount, null);
    }

    public Book(String title, String publisher, Integer publishedYear, int passageCount, String coverImageKey) {
        validateTitle(title);
        validatePublisher(publisher);
        validateCoverImageKey(coverImageKey);
        this.title = title;
        this.publisher = publisher;
        this.publishedYear = publishedYear;
        this.passageCount = passageCount;
        this.coverImageKey = coverImageKey;
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

    private static void validateCoverImageKey(String coverImageKey) {
        if (coverImageKey != null
                && (coverImageKey.length() > MAX_COVER_IMAGE_KEY_LENGTH
                || !COVER_IMAGE_KEY_PATTERN.matcher(coverImageKey).matches())) {
            throw new IllegalArgumentException("유효하지 않은 표지 이미지 키입니다.");
        }
    }
}
