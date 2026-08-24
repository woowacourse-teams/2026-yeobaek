package yeobaek.backend.book.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import yeobaek.backend.book.domain.Author;
import yeobaek.backend.book.domain.AuthorBook;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.support.BadRequestException;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.IntegrationTest;
import yeobaek.backend.support.NotFoundException;

class ActiveBookRepositoryTest extends IntegrationTest {

    @Autowired
    private ActiveBookRepository activeBookRepository;

    @Autowired
    private BookArchiveRepository bookArchiveRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private AuthorBookRepository authorBookRepository;

    @Test
    @DisplayName("활성 도서 목록과 검색에서 삭제된 도서를 제외한다")
    void excludesDeletedBooksFromListAndSearch() {
        Book active = bookArchiveRepository.save(new Book("활성 도서", null, null, 1));
        Book deleted = bookArchiveRepository.save(new Book("삭제 도서", null, null, 1));
        Author author = authorRepository.save(new Author("검색 작가"));
        authorBookRepository.save(new AuthorBook(author, deleted));
        bookArchiveRepository.delete(deleted.getId());

        assertThat(activeBookRepository.findAll()).extracting(Book::getId).containsExactly(active.getId());
        assertThat(activeBookRepository.searchByTitleOrAuthorName("삭제")).isEmpty();
        assertThat(activeBookRepository.searchByTitleOrAuthorName("검색")).isEmpty();
    }

    @Test
    @DisplayName("활성 도서 단건 조회는 미존재와 삭제 상태를 구분한다")
    void distinguishesMissingAndDeletedBook() {
        Book deleted = bookArchiveRepository.save(new Book("삭제 도서", null, null, 1));
        bookArchiveRepository.delete(deleted.getId());

        assertThatThrownBy(() -> activeBookRepository.getById(999L))
                .isInstanceOf(NotFoundException.class)
                .extracting("code").isEqualTo(ErrorCode.BOOK_NOT_FOUND);
        assertThatThrownBy(() -> activeBookRepository.getById(deleted.getId()))
                .isInstanceOf(BadRequestException.class)
                .extracting("code").isEqualTo(ErrorCode.BOOK_NOT_AVAILABLE);
    }
}
