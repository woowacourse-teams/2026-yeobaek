package yeobaek.backend.admin.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import yeobaek.backend.admin.dto.AdminAuthorResponse;
import yeobaek.backend.admin.dto.AdminAuthorsResponse;
import yeobaek.backend.book.domain.Author;
import yeobaek.backend.book.domain.AuthorBook;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.repository.AuthorBookRepository;
import yeobaek.backend.book.repository.AuthorRepository;
import yeobaek.backend.book.repository.BookRepository;
import yeobaek.backend.support.IntegrationTest;

class AdminAuthorServiceTest extends IntegrationTest {

    @Autowired
    private AdminAuthorService adminAuthorService;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorBookRepository authorBookRepository;

    @Test
    @DisplayName("작가 목록을 등록순으로 작품과 함께 조회한다")
    void findAuthorsWithBooks() {
        Author first = authorRepository.save(new Author("현진건", "000000012345964X"));
        Author second = authorRepository.save(new Author("작자 미상"));
        Book book = bookRepository.save(new Book("운수 좋은 날", "자체 제작", 1924, 1));
        authorBookRepository.save(new AuthorBook(first, book));

        AdminAuthorsResponse response = adminAuthorService.findAuthors();

        assertThat(response.authors()).extracting(AdminAuthorResponse::authorId)
                .containsExactly(first.getId(), second.getId());
        assertThat(response.authors().getFirst().isni()).isEqualTo("000000012345964X");
        assertThat(response.authors().getFirst().books()).hasSize(1);
        assertThat(response.authors().getFirst().books().getFirst().title()).isEqualTo("운수 좋은 날");
        assertThat(response.authors().getLast().books()).isEmpty();
    }

    @Test
    @DisplayName("작가가 없으면 빈 목록을 반환한다")
    void findAuthorsWhenEmpty() {
        assertThat(adminAuthorService.findAuthors().authors()).isEqualTo(List.of());
    }
}
