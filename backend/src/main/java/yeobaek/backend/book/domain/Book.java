package yeobaek.backend.book.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.BatchSize;
import yeobaek.backend.book.domain.vo.BookDeduplicationKey;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.book.domain.vo.BookDescription;
import yeobaek.backend.book.domain.vo.BodyCharacterCount;
import yeobaek.backend.book.domain.vo.PassageCount;
import yeobaek.backend.book.domain.vo.Publisher;
import yeobaek.backend.support.BadRequestException;
import yeobaek.backend.support.ErrorCode;

@Entity
@Table(name = "books")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Book {

    public static final int MAX_HASHTAG_COUNT = 5;
    public static final int MAX_HASHTAG_LENGTH = 10;
    private static final int METADATA_BATCH_SIZE = 100;
    private static final int MAX_COVER_IMAGE_KEY_LENGTH = 80;
    private static final Pattern COVER_IMAGE_ID_PATTERN = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    private static final Pattern COVER_IMAGE_KEY_PATTERN = Pattern.compile(
            "^[^/]+(?:/[^/]+)*/book-covers/"
                    + "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp)$");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    @AttributeOverride(name = "value",
            column = @Column(name = "title", nullable = false, length = BookTitle.MAX_LENGTH))
    private BookTitle title;

    @Embedded
    @AttributeOverride(name = "value",
            column = @Column(name = "publisher", length = Publisher.MAX_LENGTH))
    private Publisher publisher;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "description", columnDefinition = "TEXT"))
    private BookDescription description;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @BatchSize(size = METADATA_BATCH_SIZE)
    private List<BookGenre> genres = new ArrayList<>();

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @BatchSize(size = METADATA_BATCH_SIZE)
    private List<BookHashtag> hashtags = new ArrayList<>();

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "body_character_count"))
    private BodyCharacterCount bodyCharacterCount;

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

    public Book(BookTitle title, Publisher publisher, Integer publishedYear, int passageCount, String coverImageKey,
                BookDescription description, BodyCharacterCount bodyCharacterCount) {
        this(title, publisher, publishedYear, passageCount, coverImageKey, description, bodyCharacterCount, null, null);
    }

    public Book(BookTitle title, Publisher publisher, Integer publishedYear, int passageCount, String coverImageKey,
                BookDescription description, BodyCharacterCount bodyCharacterCount, List<String> genres,
                List<String> hashtags) {
        validateCoverImageKey(coverImageKey);
        if (title == null) {
            throw new IllegalArgumentException("도서 제목은 필수입니다.");
        }
        this.title = title;
        this.publisher = publisher;
        this.publishedYear = publishedYear;
        this.passageCount = new PassageCount(passageCount);
        this.coverImageKey = coverImageKey;
        this.description = description;
        this.bodyCharacterCount = bodyCharacterCount;
        this.genres = createGenres(genres);
        this.hashtags = createHashtags(hashtags);
    }

    public void replaceGenres(List<String> genres) {
        ensureAvailable();
        List<BookGenre> replacement = createGenres(genres);
        this.genres.clear();
        this.genres.addAll(replacement);
    }

    public void replaceHashtags(List<String> hashtags) {
        ensureAvailable();
        List<BookHashtag> replacement = createHashtags(hashtags);
        this.hashtags.clear();
        this.hashtags.addAll(replacement);
    }

    public List<String> hashtagValues() {
        return hashtags.stream().map(BookHashtag::getValue).toList();
    }

    public List<String> genreValues() {
        return genres.stream().map(BookGenre::getValue).toList();
    }

    public List<BookGenre> getGenres() {
        return List.copyOf(genres);
    }

    public List<BookHashtag> getHashtags() {
        return List.copyOf(hashtags);
    }

    private List<BookGenre> createGenres(List<String> values) {
        List<String> snapshot = values == null ? List.of() : new ArrayList<>(values);
        if (snapshot.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("장르는 null일 수 없습니다.");
        }
        List<BookGenre> result = new ArrayList<>();
        for (int position = 0; position < snapshot.size(); position++) {
            result.add(new BookGenre(this, position, snapshot.get(position)));
        }
        return result;
    }

    private List<BookHashtag> createHashtags(List<String> values) {
        List<String> snapshot = values == null ? List.of() : new ArrayList<>(values);
        if (snapshot.size() > MAX_HASHTAG_COUNT) {
            throw new IllegalArgumentException("해시태그는 최대 5개까지 등록할 수 있습니다.");
        }
        for (String value : snapshot) {
            if (value == null || value.codePointCount(0, value.length()) > MAX_HASHTAG_LENGTH) {
                throw new IllegalArgumentException("해시태그는 각각 최대 10자까지 등록할 수 있습니다.");
            }
        }
        List<BookHashtag> result = new ArrayList<>();
        for (int position = 0; position < snapshot.size(); position++) {
            result.add(new BookHashtag(this, position, snapshot.get(position)));
        }
        return result;
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
                    "더 이상 이용할 수 없는 도서입니다.",
                    availabilityLogContext());
        }
    }

    public Long pageCount() {
        return bodyCharacterCount == null ? null : bodyCharacterCount.pageCount();
    }

    public void replaceDescription(BookDescription description) {
        if (description == null) {
            throw new IllegalArgumentException("교체할 책 소개는 필수입니다.");
        }
        updateDescription(description);
    }

    public void removeDescription() {
        updateDescription(null);
    }

    private void updateDescription(BookDescription description) {
        ensureAvailable();
        this.description = description;
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
                || (!COVER_IMAGE_ID_PATTERN.matcher(coverImageKey).matches()
                && !COVER_IMAGE_KEY_PATTERN.matcher(coverImageKey).matches()))) {
            throw new IllegalArgumentException("유효하지 않은 표지 이미지 키입니다.");
        }
    }

    private Map<String, String> availabilityLogContext() {
        Map<String, String> context = new HashMap<>();
        if (id != null) {
            context.put("bookId", id.toString());
        }
        context.put("bookStatus", status.name());
        return context;
    }
}
