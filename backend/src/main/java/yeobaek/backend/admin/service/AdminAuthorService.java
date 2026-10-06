package yeobaek.backend.admin.service;

import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.RESULT;
import static yeobaek.backend.support.LogField.SUCCESS;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.admin.dto.AdminAuthorBookResponse;
import yeobaek.backend.admin.dto.AdminAuthorResponse;
import yeobaek.backend.admin.dto.AdminAuthorsResponse;
import yeobaek.backend.content.api.book.BookAdministrationApi;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAuthorService {

    private final BookAdministrationApi bookAdministrationApi;

    @Transactional(readOnly = true)
    public AdminAuthorsResponse findAuthors() {
        log.atInfo().addKeyValue(OPERATION, "admin.author.findAuthors")
                .log("관리자 작가 목록을 조회합니다.");
        List<BookAdministrationApi.AuthorView> authors = bookAdministrationApi.findAuthors();
        if (authors.isEmpty()) {
            logSuccess(0);
            return new AdminAuthorsResponse(List.of());
        }
        var response = new AdminAuthorsResponse(authors.stream()
                .map(this::toResponse)
                .toList());
        logSuccess(response.authors().size());
        return response;
    }

    private void logSuccess(int resultCount) {
        log.atInfo().addKeyValue(OPERATION, "admin.author.findAuthors").addKeyValue(RESULT, SUCCESS)
                .addKeyValue("resultCount", resultCount).log("관리자 작가 목록을 조회했습니다.");
    }

    private AdminAuthorResponse toResponse(BookAdministrationApi.AuthorView author) {
        return new AdminAuthorResponse(author.authorId(), author.name(), author.isni(), author.books().stream()
                .map(book -> new AdminAuthorBookResponse(
                        book.bookId(), book.title(), book.coverImageUrl(), book.status()))
                .toList());
    }
}
