package yeobaek.backend.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import yeobaek.backend.admin.dto.AdminDashboardBookResponse;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.BookStatus;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.book.repository.BookManagementRepository;
import yeobaek.backend.club.domain.Club;
import yeobaek.backend.club.domain.vo.ClubName;
import yeobaek.backend.club.domain.vo.JoinCode;
import yeobaek.backend.club.repository.ClubRepository;
import yeobaek.backend.support.IntegrationTest;

class AdminBookDashboardServiceTest extends IntegrationTest {

    @Autowired
    private AdminBookDashboardService adminBookDashboardService;

    @Autowired
    private BookManagementRepository bookRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Test
    @DisplayName("도서별 모임 수를 내림차순으로 조회하고 동률은 ID 순으로 정렬한다")
    void findBooksWithClubCounts() {
        Book first = saveBook("같은 제목");
        Book zero = saveBook("모임 없는 책");
        Book second = saveBook("같은 제목");
        Book tied = saveBook("동률 도서");
        clubRepository.save(new Club(new ClubName("첫 모임"), first, new JoinCode("BOOK01")));
        clubRepository.save(new Club(new ClubName("둘째 모임"), second, new JoinCode("BOOK02")));
        clubRepository.save(new Club(new ClubName("셋째 모임"), second, new JoinCode("BOOK03")));
        clubRepository.save(new Club(new ClubName("넷째 모임"), tied, new JoinCode("BOOK04")));
        bookRepository.delete(second.getId());

        assertThat(adminBookDashboardService.findBooksWithClubCounts().books())
                .extracting(
                        AdminDashboardBookResponse::bookId,
                        AdminDashboardBookResponse::title,
                        AdminDashboardBookResponse::status,
                        AdminDashboardBookResponse::clubCount)
                .containsExactly(
                        tuple(second.getId(), "같은 제목", BookStatus.DELETED, 2L),
                        tuple(first.getId(), "같은 제목", BookStatus.ACTIVE, 1L),
                        tuple(tied.getId(), "동률 도서", BookStatus.ACTIVE, 1L),
                        tuple(zero.getId(), "모임 없는 책", BookStatus.ACTIVE, 0L));
    }

    @Test
    @DisplayName("도서가 없으면 빈 목록을 반환한다")
    void findBooksWithClubCountsWhenEmpty() {
        assertThat(adminBookDashboardService.findBooksWithClubCounts().books()).isEmpty();
    }

    private Book saveBook(String title) {
        return bookRepository.save(new Book(new BookTitle(title), null, null, 1, null));
    }
}
