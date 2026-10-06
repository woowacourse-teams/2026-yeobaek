package yeobaek.backend.content.book.internal;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import yeobaek.backend.content.api.metadata.ContentMetadataDetailResponse;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.content.internal.metadata.ContentMetadataProvider;
import yeobaek.backend.content.api.metadata.ContentMetadataResponse;
import yeobaek.backend.content.api.ContentNotFoundException;
import yeobaek.backend.content.book.domain.AuthorBook;
import yeobaek.backend.content.book.internal.metadata.BookAuthorResponse;
import yeobaek.backend.content.book.internal.metadata.BookChapterResponse;
import yeobaek.backend.content.book.internal.metadata.BookMetadataDetailResponse;
import yeobaek.backend.content.book.internal.metadata.BookMetadataResponse;
import yeobaek.backend.content.book.internal.metadata.BookPublicationResponse;
import yeobaek.backend.content.book.internal.metadata.BookPublisherResponse;
import yeobaek.backend.content.book.persistence.Book;
import yeobaek.backend.content.book.repository.ActiveBookRepository;
import yeobaek.backend.content.book.repository.AuthorBookRepository;
import yeobaek.backend.content.book.repository.ChapterPassageRange;
import yeobaek.backend.content.book.repository.ChapterRepository;
import yeobaek.backend.content.book.repository.PassageRepository;
import yeobaek.backend.content.book.service.BookCoverUrlResolver;
import yeobaek.backend.shared.identity.ContentId;

@Component
@RequiredArgsConstructor
public class BookMetadataAdapter implements ContentMetadataProvider {

    private final ActiveBookRepository activeBookRepository;
    private final AuthorBookRepository authorBookRepository;
    private final ChapterRepository chapterRepository;
    private final PassageRepository passageRepository;
    private final BookCoverUrlResolver bookCoverUrlResolver;
    private final EntityManager entityManager;

    @Override
    public ContentKind supportedKind() {
        return ContentKind.BOOK;
    }

    @Override
    public List<? extends ContentMetadataResponse> search(String keyword) {
        List<Book> books = searchActiveBooks(keyword);
        Map<Long, List<BookAuthorResponse>> authors = authorsByBookId(books.stream()
                .map(Book::getId)
                .toList());
        return books.stream()
                .map(book -> metadata(book, authors.getOrDefault(book.getId(), List.of())))
                .toList();
    }

    @Override
    public ContentMetadataDetailResponse get(ContentId contentId) {
        Book book = findByContentId(contentId);
        List<BookAuthorResponse> authors = authorsByBookId(List.of(book.getId()))
                .getOrDefault(book.getId(), List.of());
        return detail(book, authors, chapters(book.getId()));
    }

    private List<Book> searchActiveBooks(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return activeBookRepository.findAll();
        }
        return activeBookRepository.searchByTitleOrAuthorName(keyword);
    }

    private Book findByContentId(ContentId contentId) {
        return entityManager.createQuery("""
                        select book from Book book where book.contentId = :contentId
                        """, Book.class)
                .setParameter("contentId", contentId.value())
                .getResultStream()
                .findFirst()
                .orElseThrow(() -> new ContentNotFoundException(contentId,
                        "도서 메타데이터를 조회할 컨텐츠가 존재하지 않습니다: contentId=" + contentId.value()));
    }

    private Map<Long, List<BookAuthorResponse>> authorsByBookId(List<Long> bookIds) {
        return authorBookRepository.findAllWithAuthorByBookIdIn(bookIds).stream()
                .collect(Collectors.groupingBy(authorBook -> authorBook.getBook().getId(),
                        Collectors.mapping(this::authorResponse, Collectors.toList())));
    }

    private BookAuthorResponse authorResponse(AuthorBook authorBook) {
        return new BookAuthorResponse(authorBook.getAuthor().getName().value());
    }

    private List<BookChapterResponse> chapters(Long bookId) {
        Map<Long, ChapterPassageRange> ranges = passageRepository.findChapterRangesByBookId(bookId).stream()
                .collect(Collectors.toMap(ChapterPassageRange::getChapterId, range -> range));
        return chapterRepository.findAllByBookIdOrderBySequenceAsc(bookId).stream()
                .map(chapter -> {
                    ChapterPassageRange range = ranges.get(chapter.getId());
                    return new BookChapterResponse(chapter.getId(), chapter.getTitle(), chapter.getSequence().value(),
                            range == null ? 0 : range.getStartSequence(),
                            range == null ? 0 : range.getEndSequence());
                })
                .toList();
    }

    private BookMetadataResponse metadata(Book book, List<BookAuthorResponse> authors) {
        return new BookMetadataResponse(new ContentId(book.getContentId()), book.getTitle().value(), authors,
                publication(book), bookCoverUrlResolver.resolve(book.getCoverImageKey()),
                book.getPassageCount().value());
    }

    private BookMetadataDetailResponse detail(
            Book book,
            List<BookAuthorResponse> authors,
            List<BookChapterResponse> chapters
    ) {
        return new BookMetadataDetailResponse(new ContentId(book.getContentId()), book.getTitle().value(), authors,
                publication(book), bookCoverUrlResolver.resolve(book.getCoverImageKey()),
                book.getPassageCount().value(), chapters);
    }

    private BookPublicationResponse publication(Book book) {
        BookPublisherResponse publisher = book.getPublisher() == null
                ? null
                : new BookPublisherResponse(book.getPublisher().value());
        return new BookPublicationResponse(publisher, book.getPublishedYear());
    }
}
