package yeobaek.backend.admin.service;

import static yeobaek.backend.support.LogField.BOOK_ID;
import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.RESULT;
import static yeobaek.backend.support.LogField.SUCCESS;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.admin.dto.AdminBookAuthorResponse;
import yeobaek.backend.admin.dto.AdminBookResponse;
import yeobaek.backend.admin.dto.AdminBooksResponse;
import yeobaek.backend.content.api.book.BookAdministrationApi;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminBookService {

    private final BookAdministrationApi bookAdministrationApi;

    @Transactional(readOnly = true)
    public AdminBooksResponse findBooks() {
        logAttempt("admin.book.findBooks", null);
        List<BookAdministrationApi.BookView> books = bookAdministrationApi.findBooks();
        if (books.isEmpty()) {
            logSuccess("admin.book.findBooks", null, 0);
            return new AdminBooksResponse(List.of());
        }
        var response = new AdminBooksResponse(books.stream()
                .map(this::toResponse)
                .toList());
        logSuccess("admin.book.findBooks", null, response.books().size());
        return response;
    }

    @Transactional
    public void delete(Long bookId) {
        logAttempt("admin.book.delete", bookId);
        bookAdministrationApi.delete(bookId);
        logSuccess("admin.book.delete", bookId);
    }

    @Transactional
    public void replaceCoverImage(Long bookId, String coverImageKey) {
        logAttempt("admin.book.replaceCoverImage", bookId);
        bookAdministrationApi.replaceCoverImage(bookId, coverImageKey);
        logSuccess("admin.book.replaceCoverImage", bookId);
    }

    @Transactional
    public void removeCoverImage(Long bookId) {
        logAttempt("admin.book.removeCoverImage", bookId);
        bookAdministrationApi.removeCoverImage(bookId);
        logSuccess("admin.book.removeCoverImage", bookId);
    }

    private AdminBookResponse toResponse(BookAdministrationApi.BookView book) {
        return new AdminBookResponse(book.bookId(), book.title(), book.authors().stream()
                .map(author -> new AdminBookAuthorResponse(author.authorId(), author.name(), author.isni()))
                .toList(), book.publisher(), book.publishedYear(), book.unitCount(),
                book.coverImageUrl(), book.status());
    }

    private void logAttempt(String operation, Long bookId) {
        log.atInfo().addKeyValue(OPERATION, operation)
                .addKeyValue(BOOK_ID, bookId).log("관리자 도서 작업을 시작합니다.");
    }

    private void logSuccess(String operation, Long bookId, int resultCount) {
        log.atInfo().addKeyValue(OPERATION, operation).addKeyValue(RESULT, SUCCESS)
                .addKeyValue(BOOK_ID, bookId).addKeyValue("resultCount", resultCount)
                .log("관리자 도서 작업을 완료했습니다.");
    }

    private void logSuccess(String operation, Long bookId) {
        log.atInfo().addKeyValue(OPERATION, operation).addKeyValue(RESULT, SUCCESS)
                .addKeyValue(BOOK_ID, bookId).log("관리자 도서 작업을 완료했습니다.");
    }
}
