package yeobaek.backend.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.vo.BookHashtags;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.book.dto.BookDetailResponse;
import yeobaek.backend.book.dto.BookSummaryResponse;
import yeobaek.backend.book.repository.BookManagementRepository;
import yeobaek.backend.club.dto.ClubBookResponse;
import yeobaek.backend.support.BadRequestException;
import yeobaek.backend.support.IntegrationTest;

class AdminBookMetadataServiceTest extends IntegrationTest {

    @Autowired
    private AdminBookService adminBookService;

    @Autowired
    private BookManagementRepository bookRepository;

    @Test
    @DisplayName("JSON 메타데이터 교체·제거를 저장하며 다른 필드는 유지한다")
    void replacesAndClearsMetadataIndependently() {
        Book book = bookRepository.save(new Book(new BookTitle("태그 도서"), null, null, 1, null, null, null,
                "비극", new BookHashtags(List.of("첫사랑", "#파국"))));
        Long id = book.getId();

        adminBookService.replaceHashtags(id, new BookHashtags(List.of("재회", "재회", "오해")));
        Book replaced = bookRepository.getById(id);
        assertThat(replaced).extracting(Book::getCategory, Book::hashtagValues)
                .containsExactly("비극", List.of("재회", "재회", "오해"));

        adminBookService.replaceCategory(id, "");
        assertThat(bookRepository.getById(id)).extracting(Book::getCategory, Book::hashtagValues)
                .containsExactly(null, List.of("재회", "재회", "오해"));

        adminBookService.replaceCategory(id, "로맨스");
        adminBookService.replaceHashtags(id, new BookHashtags(List.of()));
        Book cleared = bookRepository.getById(id);
        assertThat(cleared).extracting(Book::getCategory, Book::hashtagValues, Book::getHashtags)
                .containsExactly("로맨스", List.of(), null);
    }

    @Test
    @DisplayName("기존 도서의 부재 메타데이터는 null 장르·빈 태그 목록으로 조회한다")
    void legacyBookResponsesHaveEmptyMetadata() {
        Book book = bookRepository.save(new Book(new BookTitle("기존 도서"), null, null, 1, null, null, null));

        var summary = BookSummaryResponse.of(book, List.of(), null);
        var detail = BookDetailResponse.of(book, List.of(), null, List.of());
        var club = ClubBookResponse.of(book, List.of(), null);
        assertThat(summary).extracting(BookSummaryResponse::category, BookSummaryResponse::hashtags)
                .containsExactly(null, List.of());
        assertThat(detail).extracting(BookDetailResponse::category, BookDetailResponse::hashtags)
                .containsExactly(null, List.of());
        assertThat(club).extracting(ClubBookResponse::category, ClubBookResponse::hashtags)
                .containsExactly(null, List.of());
        assertThat(adminBookService.findBooks().books().getFirst().hashtags()).isEmpty();
    }

    @Test
    @DisplayName("삭제된 도서의 메타데이터는 변경하지 않는다")
    void rejectsChangesToDeletedBook() {
        Book book = bookRepository.save(new Book(new BookTitle("삭제 도서"), null, null, 1, null, null, null));
        adminBookService.delete(book.getId());

        assertThatThrownBy(() -> adminBookService.replaceCategory(book.getId(), "로맨스"))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> adminBookService.replaceHashtags(book.getId(), new BookHashtags(List.of("재회"))))
                .isInstanceOf(BadRequestException.class);
    }
}
