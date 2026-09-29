package yeobaek.backend.book.service;

import static yeobaek.backend.support.LogField.ATTEMPT;
import static yeobaek.backend.support.LogField.BOOK_ID;
import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.PHASE;
import static yeobaek.backend.support.LogField.SUCCESS;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.dto.BookDetailResponse;
import yeobaek.backend.book.dto.BookSummaryResponse;
import yeobaek.backend.book.dto.BooksResponse;
import yeobaek.backend.book.dto.ChapterResponse;
import yeobaek.backend.book.repository.AuthorBookRepository;
import yeobaek.backend.book.repository.ActiveBookRepository;
import yeobaek.backend.book.repository.ChapterPassageRange;
import yeobaek.backend.book.repository.ChapterRepository;
import yeobaek.backend.book.repository.PassageRepository;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BookService {

    private final ActiveBookRepository bookRepository;
    private final AuthorBookRepository authorBookRepository;
    private final ChapterRepository chapterRepository;
    private final PassageRepository passageRepository;
    private final BookCoverUrlResolver bookCoverUrlResolver;

    public BooksResponse findBooks(String keyword) {
        boolean searchUsed = keyword != null && !keyword.isBlank();
        log.atInfo().addKeyValue(OPERATION, "book.findAll").addKeyValue(PHASE, ATTEMPT)
                .addKeyValue("searchUsed", searchUsed).log("도서 목록을 조회합니다.");
        List<Book> books = search(keyword);
        Map<Long, List<String>> authorNames = authorNamesByBookId(books.stream().map(Book::getId).toList());
        var response = new BooksResponse(books.stream()
                .map(book -> BookSummaryResponse.of(book, authorNames.getOrDefault(book.getId(), List.of()),
                        bookCoverUrlResolver.resolve(book.getCoverImageKey())))
                .toList());
        log.atInfo().addKeyValue(OPERATION, "book.findAll").addKeyValue(PHASE, SUCCESS)
                .addKeyValue("searchUsed", searchUsed).addKeyValue("resultCount", response.books().size())
                .log("도서 목록을 조회했습니다.");
        return response;
    }

    private List<Book> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return bookRepository.findAll();
        }
        return bookRepository.searchByTitleOrAuthorName(keyword);
    }

    public BookDetailResponse findBook(Long bookId) {
        log.atInfo().addKeyValue(OPERATION, "book.findOne").addKeyValue(PHASE, ATTEMPT)
                .addKeyValue(BOOK_ID, bookId).log("도서 상세를 조회합니다.");
        Book book = bookRepository.getById(bookId);
        List<String> authors = authorNamesByBookId(List.of(bookId)).getOrDefault(bookId, List.of());
        var response = new BookDetailResponse(book.getId(), book.getTitle().value(), authors,
                book.getPublisher() == null ? null : book.getPublisher().value(), book.getPublishedYear(),
                bookCoverUrlResolver.resolve(book.getCoverImageKey()),
                book.getPassageCount().value(), chapters(bookId));
        log.atInfo().addKeyValue(OPERATION, "book.findOne").addKeyValue(PHASE, SUCCESS)
                .addKeyValue(BOOK_ID, bookId).addKeyValue("chapterCount", response.chapters().size())
                .log("도서 상세를 조회했습니다.");
        return response;
    }

    private List<ChapterResponse> chapters(Long bookId) {
        Map<Long, ChapterPassageRange> ranges = passageRepository.findChapterRangesByBookId(bookId).stream()
                .collect(Collectors.toMap(ChapterPassageRange::getChapterId, range -> range));
        return chapterRepository.findAllByBookIdOrderBySequenceAsc(bookId).stream()
                .map(chapter -> {
                    ChapterPassageRange range = ranges.get(chapter.getId());
                    return new ChapterResponse(chapter.getId(), chapter.getTitle(), chapter.getSequence().value(),
                            range == null ? 0 : range.getStartSequence(),
                            range == null ? 0 : range.getEndSequence());
                })
                .toList();
    }

    private Map<Long, List<String>> authorNamesByBookId(List<Long> bookIds) {
        return authorBookRepository.findAllWithAuthorByBookIdIn(bookIds).stream()
                .collect(Collectors.groupingBy(authorBook -> authorBook.getBook().getId(),
                        Collectors.mapping(authorBook -> authorBook.getAuthor().getName().value(), Collectors.toList())));
    }
}
