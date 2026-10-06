package yeobaek.backend.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import yeobaek.backend.content.api.book.BookAdministrationApi;
import yeobaek.backend.support.LogCapture;

class AdminBookLoggingTest {

    private final BookAdministrationApi books = mock(BookAdministrationApi.class);
    private final AdminBookService service = new AdminBookService(books);

    @Test
    @DisplayName("관리자 도서 삭제 완료 로그는 도서 ID를 포함하고 임의 건수를 제외한다")
    void logBookDeletion() {
        try (var logs = new LogCapture(AdminBookService.class.getName())) {
            service.delete(3L);

            var event = logs.event("admin.book.delete", "success");
            assertThat(logs.field(event, "bookId")).isEqualTo(3L);
            assertThat(logs.hasField(event, "resultCount")).isFalse();
        }
    }

    @Test
    @DisplayName("관리자 표지 변경 완료 로그는 도서 ID를 포함하고 임의 건수를 제외한다")
    void logCoverReplacement() {
        try (var logs = new LogCapture(AdminBookService.class.getName())) {
            service.replaceCoverImage(5L, "yeobaek/book-covers/example.jpg");

            var event = logs.event("admin.book.replaceCoverImage", "success");
            assertThat(logs.field(event, "bookId")).isEqualTo(5L);
            assertThat(logs.hasField(event, "resultCount")).isFalse();
        }
    }
}
