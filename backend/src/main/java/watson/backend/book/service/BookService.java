package watson.backend.book.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import watson.backend.book.domain.Book;
import watson.backend.book.dto.BookDetailResponse;
import watson.backend.book.dto.BookSummaryResponse;
import watson.backend.book.dto.BooksResponse;
import watson.backend.book.dto.ChapterResponse;
import watson.backend.book.repository.AuthorBookRepository;
import watson.backend.book.repository.BookRepository;
import watson.backend.book.repository.ChapterPassageRange;
import watson.backend.book.repository.ChapterRepository;
import watson.backend.book.repository.PassageRepository;
import watson.backend.support.ErrorCode;
import watson.backend.support.NotFoundException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;
    private final AuthorBookRepository authorBookRepository;
    private final ChapterRepository chapterRepository;
    private final PassageRepository passageRepository;

    public BooksResponse findBooks() {
        List<Book> books = bookRepository.findAll();
        Map<Long, List<String>> authorNames = authorNamesByBookId(books.stream().map(Book::getId).toList());
        return new BooksResponse(books.stream()
                .map(book -> BookSummaryResponse.of(book, authorNames.getOrDefault(book.getId(), List.of())))
                .toList());
    }

    public BookDetailResponse findBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.BOOK_NOT_FOUND));
        List<String> authors = authorNamesByBookId(List.of(bookId)).getOrDefault(bookId, List.of());
        return new BookDetailResponse(book.getId(), book.getTitle(), authors,
                book.getPublisher(), book.getPublishedYear(), book.getPassageCount(), chapters(bookId));
    }

    private List<ChapterResponse> chapters(Long bookId) {
        Map<Long, ChapterPassageRange> ranges = passageRepository.findChapterRangesByBookId(bookId).stream()
                .collect(Collectors.toMap(ChapterPassageRange::getChapterId, range -> range));
        return chapterRepository.findAllByBookIdOrderBySequenceAsc(bookId).stream()
                .map(chapter -> {
                    ChapterPassageRange range = ranges.get(chapter.getId());
                    return new ChapterResponse(chapter.getId(), chapter.getTitle(), chapter.getSequence(),
                            range == null ? 0 : range.getStartSequence(),
                            range == null ? 0 : range.getEndSequence());
                })
                .toList();
    }

    private Map<Long, List<String>> authorNamesByBookId(List<Long> bookIds) {
        return authorBookRepository.findAllWithAuthorByBookIdIn(bookIds).stream()
                .collect(Collectors.groupingBy(authorBook -> authorBook.getBook().getId(),
                        Collectors.mapping(authorBook -> authorBook.getAuthor().getName(), Collectors.toList())));
    }
}
