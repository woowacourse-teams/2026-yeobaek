package yeobaek.backend.admin.service;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import yeobaek.backend.content.api.book.BookAdministrationApi;

@ExtendWith(MockitoExtension.class)
class AdminBookLockingTest {

    private static final Long BOOK_ID = 1L;
    private static final String COVER_KEY = "yeobaek/book-covers/123e4567-e89b-12d3-a456-426614174000.jpg";

    @Mock
    private BookAdministrationApi bookAdministrationApi;

    @InjectMocks
    private AdminBookService adminBookService;

    @Test
    @DisplayName("표지 교체를 도서 관리 API에 위임한다")
    void replaceCoverImageThroughAdministrationApi() {
        adminBookService.replaceCoverImage(BOOK_ID, COVER_KEY);
        verify(bookAdministrationApi).replaceCoverImage(BOOK_ID, COVER_KEY);
    }

    @Test
    @DisplayName("표지 제거를 도서 관리 API에 위임한다")
    void removeCoverImageThroughAdministrationApi() {
        adminBookService.removeCoverImage(BOOK_ID);
        verify(bookAdministrationApi).removeCoverImage(BOOK_ID);
    }
}
