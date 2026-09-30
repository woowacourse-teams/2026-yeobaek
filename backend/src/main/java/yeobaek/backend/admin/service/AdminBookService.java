package yeobaek.backend.admin.service;

import static yeobaek.backend.support.LogField.BOOK_ID;
import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.RESULT;
import static yeobaek.backend.support.LogField.SUCCESS;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.admin.dto.AdminBookAuthorResponse;
import yeobaek.backend.admin.dto.AdminBookResponse;
import yeobaek.backend.admin.dto.AdminBooksResponse;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.repository.AuthorBookRepository;
import yeobaek.backend.book.repository.BookManagementRepository;
import yeobaek.backend.book.service.BookCoverUrlResolver;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminBookService {

    private final BookManagementRepository bookManagementRepository;
    private final AuthorBookRepository authorBookRepository;
    private final BookCoverUrlResolver bookCoverUrlResolver;

    @Transactional(readOnly = true)
    public AdminBooksResponse findBooks() {
        logAttempt("admin.book.findBooks", null);
        List<Book> books = bookManagementRepository.findAllByOrderByIdAsc();
        if (books.isEmpty()) {
            logSuccess("admin.book.findBooks", null, 0);
            return new AdminBooksResponse(List.of());
        }
        Map<Long, List<AdminBookAuthorResponse>> authorsByBookId = authorsByBookId(
                books.stream().map(Book::getId).toList());
        var response = new AdminBooksResponse(books.stream()
                .map(book -> AdminBookResponse.of(
                        book,
                        authorsByBookId.getOrDefault(book.getId(), List.of()),
                        bookCoverUrlResolver.resolve(book.getCoverImageKey())))
                .toList());
        logSuccess("admin.book.findBooks", null, response.books().size());
        return response;
    }

    @Transactional
    public void delete(Long bookId) {
        logAttempt("admin.book.delete", bookId);
        bookManagementRepository.delete(bookId);
        logSuccess("admin.book.delete", bookId);
    }

    @Transactional
    public void replaceCoverImage(Long bookId, String coverImageKey) {
        logAttempt("admin.book.replaceCoverImage", bookId);
        bookManagementRepository.getByIdForUpdate(bookId).replaceCoverImage(coverImageKey);
        logSuccess("admin.book.replaceCoverImage", bookId);
    }

    @Transactional
    public void removeCoverImage(Long bookId) {
        logAttempt("admin.book.removeCoverImage", bookId);
        bookManagementRepository.getByIdForUpdate(bookId).removeCoverImage();
        logSuccess("admin.book.removeCoverImage", bookId);
    }

    private Map<Long, List<AdminBookAuthorResponse>> authorsByBookId(List<Long> bookIds) {
        return authorBookRepository.findAllWithAuthorByBookIdIn(bookIds).stream()
                .collect(Collectors.groupingBy(
                        authorBook -> authorBook.getBook().getId(),
                        Collectors.mapping(
                                authorBook -> AdminBookAuthorResponse.from(authorBook.getAuthor()),
                                Collectors.toList())));
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
