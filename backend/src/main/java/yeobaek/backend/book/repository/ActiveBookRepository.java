package yeobaek.backend.book.repository;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.BookStatus;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.NotFoundException;

@Repository
@RequiredArgsConstructor
public class ActiveBookRepository {

    private final BookJpaRepository bookJpaRepository;

    public Book getById(Long bookId) {
        Book book = bookJpaRepository.findById(bookId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.BOOK_NOT_FOUND));
        book.ensureAvailable();
        return book;
    }

    public List<Book> findAll() {
        return bookJpaRepository.findAllByStatusOrderByIdAsc(BookStatus.ACTIVE);
    }

    public List<Book> searchByTitleOrAuthorName(String keyword) {
        return bookJpaRepository.searchActiveByTitleOrAuthorName(keyword, BookStatus.ACTIVE);
    }

    public List<Book> findAllByTitle(String title) {
        return bookJpaRepository.findAllByTitleAndStatus(title, BookStatus.ACTIVE);
    }
}
