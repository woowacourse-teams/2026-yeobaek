package yeobaek.backend.book.repository;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.NotFoundException;

@Repository
@RequiredArgsConstructor
public class BookManagementRepository {

    private final BookJpaRepository bookJpaRepository;

    public Book save(Book book) {
        return bookJpaRepository.save(book);
    }

    public Book getById(Long bookId) {
        return bookJpaRepository.findById(bookId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.BOOK_NOT_FOUND));
    }

    public Optional<Book> findById(Long bookId) {
        return bookJpaRepository.findById(bookId);
    }

    public List<Book> findAll() {
        return bookJpaRepository.findAll();
    }

    public Book getByIdForUpdate(Long bookId) {
        return bookJpaRepository.findByIdForUpdate(bookId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.BOOK_NOT_FOUND));
    }

    public long count() {
        return bookJpaRepository.count();
    }

    @Transactional
    public void delete(Long bookId) {
        getByIdForUpdate(bookId).delete();
    }
}
