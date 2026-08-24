package yeobaek.backend.book.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
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
@Transactional(readOnly = true)
public class BookService {

    private final ActiveBookRepository bookRepository;
    private final AuthorBookRepository authorBookRepository;
    private final ChapterRepository chapterRepository;
    private final PassageRepository passageRepository;

    public BooksResponse findBooks(String keyword) {
        List<Book> books = search(keyword);
        Map<Long, List<String>> authorNames = authorNamesByBookId(books.stream().map(Book::getId).toList());
        return new BooksResponse(books.stream()
                .map(book -> BookSummaryResponse.of(book, authorNames.getOrDefault(book.getId(), List.of())))
                .toList());
    }

    private List<Book> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return bookRepository.findAll();
        }
        return bookRepository.searchByTitleOrAuthorName(keyword);
    }

    public BookDetailResponse findBook(Long bookId) {
        Book book = bookRepository.getById(bookId);
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
