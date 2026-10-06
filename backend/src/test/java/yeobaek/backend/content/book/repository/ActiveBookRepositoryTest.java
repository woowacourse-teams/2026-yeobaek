package yeobaek.backend.content.book.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import yeobaek.backend.content.book.domain.Author;
import yeobaek.backend.content.book.domain.AuthorBook;
import yeobaek.backend.content.book.persistence.Book;
import yeobaek.backend.content.api.value.AuthorName;
import yeobaek.backend.content.api.value.BookTitle;
import yeobaek.backend.shared.exception.BadRequestException;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.support.IntegrationTest;
import yeobaek.backend.shared.exception.NotFoundException;

class ActiveBookRepositoryTest extends IntegrationTest {

    @Autowired
    private ActiveBookRepository activeBookRepository;

    @Autowired
    private BookManagementRepository bookManagementRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private AuthorBookRepository authorBookRepository;

    @Test
    @DisplayName("삭제된 도서는 활성 도서 목록에 포함되지 않는다")
    void excludesDeletedBookFromActiveBooks() {
        Book active = bookManagementRepository.save(newBook(new BookTitle("활성 도서"), null, null, 1, null));
        Book deleted = bookManagementRepository.save(newBook(new BookTitle("삭제 도서"), null, null, 1, null));
        bookManagementRepository.delete(deleted.getId());

        assertThat(activeBookRepository.findAll()).extracting(Book::getId).containsExactly(active.getId());
    }

    @Test
    @DisplayName("삭제된 도서는 제목이나 작가 이름으로 검색할 수 없다")
    void excludesDeletedBookFromSearchResults() {
        Book deleted = bookManagementRepository.save(newBook(new BookTitle("삭제 도서"), null, null, 1, null));
        Author author = authorRepository.save(new Author(new AuthorName("검색 작가")));
        authorBookRepository.save(new AuthorBook(author, deleted));
        bookManagementRepository.delete(deleted.getId());

        assertThat(activeBookRepository.searchByTitleOrAuthorName("삭제")).isEmpty();
        assertThat(activeBookRepository.searchByTitleOrAuthorName("검색")).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 도서는 BOOK_NOT_FOUND 오류로 구분한다")
    void distinguishesMissingBook() {
        assertThatThrownBy(() -> activeBookRepository.getById(999L))
                .isInstanceOf(NotFoundException.class)
                .extracting("code").isEqualTo(ErrorCode.BOOK_NOT_FOUND);
    }

    @Test
    @DisplayName("삭제된 도서는 BOOK_NOT_AVAILABLE 오류로 구분한다")
    void distinguishesDeletedBook() {
        Book deleted = bookManagementRepository.save(newBook(new BookTitle("삭제 도서"), null, null, 1, null));
        bookManagementRepository.delete(deleted.getId());

        assertThatThrownBy(() -> activeBookRepository.getById(deleted.getId()))
                .isInstanceOf(BadRequestException.class)
                .extracting("code").isEqualTo(ErrorCode.BOOK_NOT_AVAILABLE);
    }
}
