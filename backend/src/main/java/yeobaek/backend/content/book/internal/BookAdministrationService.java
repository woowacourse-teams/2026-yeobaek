package yeobaek.backend.content.book.internal;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.content.api.book.BookAdministrationApi;
import yeobaek.backend.content.book.domain.Author;
import yeobaek.backend.content.book.domain.AuthorBook;
import yeobaek.backend.content.book.persistence.Book;
import yeobaek.backend.content.book.repository.AuthorBookRepository;
import yeobaek.backend.content.book.repository.AuthorRepository;
import yeobaek.backend.content.book.repository.BookManagementRepository;
import yeobaek.backend.content.book.service.BookCoverUrlResolver;

@Service
@RequiredArgsConstructor
public class BookAdministrationService implements BookAdministrationApi {

    private final BookManagementRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final AuthorBookRepository authorBookRepository;
    private final BookCoverUrlResolver coverUrlResolver;

    @Override
    @Transactional(readOnly = true)
    public List<BookView> findBooks() {
        List<Book> books = bookRepository.findAllByOrderByIdAsc();
        Map<Long, List<AuthorSummary>> authors = authorsByBookId(books.stream()
                .map(Book::getId)
                .toList());
        return books.stream()
                .map(book -> bookView(book, authors.getOrDefault(book.getId(), List.of())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuthorView> findAuthors() {
        List<Author> authors = authorRepository.findAllByOrderByIdAsc();
        Map<Long, List<AuthorBookSummary>> books = booksByAuthorId(authors.stream()
                .map(Author::getId)
                .toList());
        return authors.stream()
                .map(author -> new AuthorView(author.getId(), author.getName().value(),
                        author.getIsni() == null ? null : author.getIsni().value(),
                        books.getOrDefault(author.getId(), List.of())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookStatusView> findBookStatuses() {
        return bookRepository.findAll().stream()
                .map(book -> new BookStatusView(book.getId(), book.getTitle().value(), book.getStatus().name()))
                .toList();
    }

    @Override
    @Transactional
    public void delete(long bookId) {
        bookRepository.delete(bookId);
    }

    @Override
    @Transactional
    public void replaceCoverImage(long bookId, String coverImageKey) {
        bookRepository.getByIdForUpdate(bookId).replaceCoverImage(coverImageKey);
    }

    @Override
    @Transactional
    public void removeCoverImage(long bookId) {
        bookRepository.getByIdForUpdate(bookId).removeCoverImage();
    }

    private Map<Long, List<AuthorSummary>> authorsByBookId(List<Long> bookIds) {
        if (bookIds.isEmpty()) {
            return Map.of();
        }
        return authorBookRepository.findAllWithAuthorByBookIdIn(bookIds).stream()
                .collect(Collectors.groupingBy(authorBook -> authorBook.getBook().getId(),
                        Collectors.mapping(authorBook -> authorSummary(authorBook.getAuthor()),
                                Collectors.toList())));
    }

    private Map<Long, List<AuthorBookSummary>> booksByAuthorId(List<Long> authorIds) {
        if (authorIds.isEmpty()) {
            return Map.of();
        }
        return authorBookRepository.findAllWithBookByAuthorIdIn(authorIds).stream()
                .collect(Collectors.groupingBy(authorBook -> authorBook.getAuthor().getId(),
                        Collectors.mapping(this::bookSummary, Collectors.toList())));
    }

    private AuthorSummary authorSummary(Author author) {
        return new AuthorSummary(author.getId(), author.getName().value(),
                author.getIsni() == null ? null : author.getIsni().value());
    }

    private AuthorBookSummary bookSummary(AuthorBook authorBook) {
        Book book = authorBook.getBook();
        return new AuthorBookSummary(book.getId(), book.getTitle().value(),
                coverUrlResolver.resolve(book.getCoverImageKey()), book.getStatus().name());
    }

    private BookView bookView(Book book, List<AuthorSummary> authors) {
        return new BookView(book.getId(), book.getTitle().value(), authors,
                book.getPublisher() == null ? null : book.getPublisher().value(),
                book.getPublishedYear(), book.getPassageCount().value(),
                coverUrlResolver.resolve(book.getCoverImageKey()), book.getStatus().name());
    }
}
