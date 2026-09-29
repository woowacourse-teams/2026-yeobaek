package yeobaek.backend.book.repository;

import static yeobaek.backend.support.LogField.ATTEMPT;
import static yeobaek.backend.support.LogField.BOOK_ID;
import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.PHASE;
import static yeobaek.backend.support.LogField.SUCCESS;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.NotFoundException;

@Repository
@RequiredArgsConstructor
@Slf4j
public class BookManagementRepository {

    private final BookJpaRepository bookJpaRepository;

    public Book save(Book book) {
        logAttempt("bookManagement.save", book.getId());
        Book saved = bookJpaRepository.save(book);
        logSuccess("bookManagement.save", saved.getId(), 1);
        return saved;
    }

    public Book getById(Long bookId) {
        logAttempt("bookManagement.getOne", bookId);
        Book book = bookJpaRepository.findById(bookId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.BOOK_NOT_FOUND,
                        "도서가 존재하지 않습니다: bookId=" + bookId,
                        Map.of(BOOK_ID, bookId.toString())));
        logSuccess("bookManagement.getOne", bookId, 1);
        return book;
    }

    public Optional<Book> findById(Long bookId) {
        logAttempt("bookManagement.findOne", bookId);
        Optional<Book> book = bookJpaRepository.findById(bookId);
        logSuccess("bookManagement.findOne", bookId, book.isPresent() ? 1 : 0);
        return book;
    }

    public List<Book> findAll() {
        logAttempt("bookManagement.findAll", null);
        List<Book> books = bookJpaRepository.findAll();
        logSuccess("bookManagement.findAll", null, books.size());
        return books;
    }

    public List<Book> findAllByOrderByIdAsc() {
        logAttempt("bookManagement.findAllOrdered", null);
        List<Book> books = bookJpaRepository.findAllByOrderByIdAsc();
        logSuccess("bookManagement.findAllOrdered", null, books.size());
        return books;
    }

    public Book getByIdForUpdate(Long bookId) {
        logAttempt("bookManagement.getOneForUpdate", bookId);
        Book book = bookJpaRepository.findByIdForUpdate(bookId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.BOOK_NOT_FOUND,
                        "도서가 존재하지 않습니다: bookId=" + bookId,
                        Map.of(BOOK_ID, bookId.toString())));
        logSuccess("bookManagement.getOneForUpdate", bookId, 1);
        return book;
    }

    public long count() {
        logAttempt("bookManagement.count", null);
        long count = bookJpaRepository.count();
        logSuccess("bookManagement.count", null, count);
        return count;
    }

    @Transactional
    public void delete(Long bookId) {
        logAttempt("bookManagement.delete", bookId);
        getByIdForUpdate(bookId).delete();
        logSuccess("bookManagement.delete", bookId, 1);
    }

    private void logAttempt(String operation, Long bookId) {
        log.atInfo().addKeyValue(OPERATION, operation).addKeyValue(PHASE, ATTEMPT)
                .addKeyValue(BOOK_ID, bookId).log("도서 관리 저장소를 호출합니다.");
    }

    private void logSuccess(String operation, Long bookId, long resultCount) {
        log.atInfo().addKeyValue(OPERATION, operation).addKeyValue(PHASE, SUCCESS)
                .addKeyValue(BOOK_ID, bookId).addKeyValue("resultCount", resultCount)
                .log("도서 관리 저장소 호출을 완료했습니다.");
    }
}
