package yeobaek.backend.admin.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.vo.BookGenres;
import yeobaek.backend.book.domain.vo.BookHashtags;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.book.repository.BookManagementRepository;
import yeobaek.backend.support.IntegrationTest;

class AdminBookMetadataServiceTest extends IntegrationTest {

    @Autowired
    private AdminBookService adminBookService;

    @Autowired
    private BookManagementRepository bookRepository;

    @Test
    @DisplayName("두 JSON 목록을 독립 교체·제거하고 다시 조회한다")
    void persistsMetadataIndependently() {
        Book book = bookRepository.save(new Book(new BookTitle("태그 도서"), null, null, 1, null, null, null,
                new BookGenres(List.of("로맨스", "비극")), new BookHashtags(List.of("첫사랑", "#파국"))));
        Long id = book.getId();

        adminBookService.replaceHashtags(id, new BookHashtags(List.of("재회", "재회", "오해")));
        assertThat(bookRepository.getById(id)).extracting(Book::genreValues, Book::hashtagValues)
                .containsExactly(List.of("로맨스", "비극"), List.of("재회", "재회", "오해"));

        adminBookService.replaceGenres(id, new BookGenres(List.of()));
        assertThat(bookRepository.getById(id)).extracting(Book::getGenres, Book::hashtagValues)
                .containsExactly(null, List.of("재회", "재회", "오해"));

        adminBookService.replaceGenres(id, new BookGenres(List.of("로맨스", "성장")));
        adminBookService.replaceHashtags(id, new BookHashtags(List.of()));
        assertThat(bookRepository.getById(id)).extracting(Book::genreValues, Book::getHashtags)
                .containsExactly(List.of("로맨스", "성장"), null);
    }
}
