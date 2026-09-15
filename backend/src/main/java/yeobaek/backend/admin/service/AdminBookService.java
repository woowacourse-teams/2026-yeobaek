package yeobaek.backend.admin.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
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
public class AdminBookService {

    private final BookManagementRepository bookManagementRepository;
    private final AuthorBookRepository authorBookRepository;
    private final BookCoverUrlResolver bookCoverUrlResolver;

    @Transactional(readOnly = true)
    public AdminBooksResponse findBooks() {
        List<Book> books = bookManagementRepository.findAllByOrderByIdAsc();
        if (books.isEmpty()) {
            return new AdminBooksResponse(List.of());
        }
        Map<Long, List<AdminBookAuthorResponse>> authorsByBookId = authorsByBookId(
                books.stream().map(Book::getId).toList());
        return new AdminBooksResponse(books.stream()
                .map(book -> AdminBookResponse.of(
                        book,
                        authorsByBookId.getOrDefault(book.getId(), List.of()),
                        bookCoverUrlResolver.resolve(book.getCoverImageKey())))
                .toList());
    }

    @Transactional
    public void delete(Long bookId) {
        bookManagementRepository.delete(bookId);
    }

    @Transactional
    public void replaceCoverImage(Long bookId, String coverImageKey) {
        bookManagementRepository.getByIdForUpdate(bookId).replaceCoverImage(coverImageKey);
    }

    @Transactional
    public void removeCoverImage(Long bookId) {
        bookManagementRepository.getByIdForUpdate(bookId).removeCoverImage();
    }

    private Map<Long, List<AdminBookAuthorResponse>> authorsByBookId(List<Long> bookIds) {
        return authorBookRepository.findAllWithAuthorByBookIdIn(bookIds).stream()
                .collect(Collectors.groupingBy(
                        authorBook -> authorBook.getBook().getId(),
                        Collectors.mapping(
                                authorBook -> AdminBookAuthorResponse.from(authorBook.getAuthor()),
                                Collectors.toList())));
    }
}
