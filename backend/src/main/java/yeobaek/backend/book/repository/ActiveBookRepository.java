package yeobaek.backend.book.repository;

import static yeobaek.backend.support.LogField.ATTEMPT;
import static yeobaek.backend.support.LogField.BOOK_ID;
import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.PHASE;
import static yeobaek.backend.support.LogField.SUCCESS;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.BookStatus;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.NotFoundException;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ActiveBookRepository {

    private final BookJpaRepository bookJpaRepository;

    public Book getById(Long bookId) {
        logAttempt("activeBook.getOne", bookId);
        Book book = bookJpaRepository.findById(bookId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.BOOK_NOT_FOUND,
                        "이용할 도서가 존재하지 않습니다: bookId=" + bookId,
                        Map.of(BOOK_ID, bookId.toString())));
        book.ensureAvailable();
        logSuccess("activeBook.getOne", bookId, 1);
        return book;
    }

    public List<Book> findAll() {
        logAttempt("activeBook.findAll", null);
        List<Book> books = bookJpaRepository.findAllByStatusOrderByIdAsc(BookStatus.ACTIVE);
        logSuccess("activeBook.findAll", null, books.size());
        return books;
    }

    public List<Book> searchByTitleOrAuthorName(String keyword) {
        logAttempt("activeBook.search", null);
        List<Book> books = bookJpaRepository.searchActiveByTitleOrAuthorName(keyword, BookStatus.ACTIVE);
        logSuccess("activeBook.search", null, books.size());
        return books;
    }

    public List<Book> findAllByTitle(BookTitle title) {
        logAttempt("activeBook.findByTitle", null);
        List<Book> books = bookJpaRepository.findAllByTitleAndStatus(title.value(), BookStatus.ACTIVE);
        logSuccess("activeBook.findByTitle", null, books.size());
        return books;
    }

    private void logAttempt(String operation, Long bookId) {
        log.atInfo().addKeyValue(OPERATION, operation).addKeyValue(PHASE, ATTEMPT)
                .addKeyValue(BOOK_ID, bookId).log("활성 도서 저장소를 호출합니다.");
    }

    private void logSuccess(String operation, Long bookId, int resultCount) {
        log.atInfo().addKeyValue(OPERATION, operation).addKeyValue(PHASE, SUCCESS)
                .addKeyValue(BOOK_ID, bookId).addKeyValue("resultCount", resultCount)
                .log("활성 도서 저장소 호출을 완료했습니다.");
    }
}
