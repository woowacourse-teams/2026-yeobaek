package yeobaek.backend.admin.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.repository.BookManagementRepository;

@ExtendWith(MockitoExtension.class)
class AdminBookLockingTest {

    private static final Long BOOK_ID = 1L;
    private static final String COVER_KEY = "book-covers/123e4567-e89b-12d3-a456-426614174000.jpg";

    @Mock
    private BookManagementRepository bookManagementRepository;

    @Mock
    private Book book;

    @InjectMocks
    private AdminBookService adminBookService;

    @Test
    @DisplayName("표지 교체는 삭제와 같은 행 잠금을 획득한 뒤 상태를 바꾼다")
    void replaceCoverImageWithWriteLock() {
        given(bookManagementRepository.getByIdForUpdate(BOOK_ID)).willReturn(book);

        adminBookService.replaceCoverImage(BOOK_ID, COVER_KEY);

        InOrder ordered = inOrder(bookManagementRepository, book);
        ordered.verify(bookManagementRepository).getByIdForUpdate(BOOK_ID);
        ordered.verify(book).replaceCoverImage(COVER_KEY);
    }

    @Test
    @DisplayName("표지 제거는 삭제와 같은 행 잠금을 획득한 뒤 상태를 바꾼다")
    void removeCoverImageWithWriteLock() {
        given(bookManagementRepository.getByIdForUpdate(BOOK_ID)).willReturn(book);

        adminBookService.removeCoverImage(BOOK_ID);

        InOrder ordered = inOrder(bookManagementRepository, book);
        ordered.verify(bookManagementRepository).getByIdForUpdate(BOOK_ID);
        ordered.verify(book).removeCoverImage();
    }
}
