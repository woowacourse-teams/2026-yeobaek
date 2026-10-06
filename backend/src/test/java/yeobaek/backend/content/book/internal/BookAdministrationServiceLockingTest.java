package yeobaek.backend.content.book.internal;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import yeobaek.backend.content.book.persistence.Book;
import yeobaek.backend.content.book.repository.AuthorBookRepository;
import yeobaek.backend.content.book.repository.AuthorRepository;
import yeobaek.backend.content.book.repository.BookManagementRepository;
import yeobaek.backend.content.book.service.BookCoverUrlResolver;

@ExtendWith(MockitoExtension.class)
class BookAdministrationServiceLockingTest {

    private static final long BOOK_ID = 1L;
    private static final String COVER_KEY = "yeobaek/book-covers/cover.jpg";

    @Mock private BookManagementRepository books;
    @Mock private AuthorRepository authors;
    @Mock private AuthorBookRepository authorBooks;
    @Mock private BookCoverUrlResolver coverUrls;
    @Mock private Book book;
    @InjectMocks private BookAdministrationService service;

    @Test
    void replaceCoverImageUsesWriteLockBeforeChangingState() {
        given(books.getByIdForUpdate(BOOK_ID)).willReturn(book);

        service.replaceCoverImage(BOOK_ID, COVER_KEY);

        verify(books).getByIdForUpdate(BOOK_ID);
        verify(books, never()).getById(BOOK_ID);
        verify(book).replaceCoverImage(COVER_KEY);
    }

    @Test
    void removeCoverImageUsesWriteLockBeforeChangingState() {
        given(books.getByIdForUpdate(BOOK_ID)).willReturn(book);

        service.removeCoverImage(BOOK_ID);

        verify(books).getByIdForUpdate(BOOK_ID);
        verify(books, never()).getById(BOOK_ID);
        verify(book).removeCoverImage();
    }
}
