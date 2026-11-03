package yeobaek.backend.admin.service;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import yeobaek.backend.admin.dto.AdminBookResponse;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.book.repository.BookManagementRepository;
import yeobaek.backend.support.IntegrationTest;

class AdminBookMetadataServiceTest extends IntegrationTest {

    @Autowired
    private AdminBookService adminBookService;

    @Autowired
    private BookManagementRepository bookRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    @DisplayName("책 소유 목록을 독립 교체·제거하고 이전 자식 행도 제거한다")
    void persistsMetadataIndependently() {
        Book book = bookRepository.save(new Book(new BookTitle("태그 도서"), null, null, 1, null, null, null,
                List.of("로맨스", "비극"), List.of("첫사랑", "#파국")));
        Long id = book.getId();
        List<Long> initialTagIds = jdbcTemplate.queryForList(
                "SELECT id FROM book_hashtags WHERE book_id = ?", Long.class, id);
        assertThat(initialTagIds).hasSize(2);

        adminBookService.replaceHashtags(id, List.of("재회", "재회", "오해"));
        assertThat(adminBookService.findBooks().books().getFirst())
                .extracting(AdminBookResponse::genres, AdminBookResponse::hashtags)
                .containsExactly(List.of("로맨스", "비극"), List.of("재회", "재회", "오해"));
        assertThat(jdbcTemplate.queryForList("SELECT id FROM book_hashtags WHERE book_id = ?", Long.class, id))
                .doesNotContainAnyElementsOf(initialTagIds);

        adminBookService.replaceGenres(id, List.of());
        assertThat(adminBookService.findBooks().books().getFirst())
                .extracting(AdminBookResponse::genres, AdminBookResponse::hashtags)
                .containsExactly(List.of(), List.of("재회", "재회", "오해"));

        adminBookService.replaceGenres(id, List.of("로맨스", "성장"));
        adminBookService.replaceHashtags(id, List.of());
        assertThat(adminBookService.findBooks().books().getFirst())
                .extracting(AdminBookResponse::genres, AdminBookResponse::hashtags)
                .containsExactly(List.of("로맨스", "성장"), List.of());
    }

    @Test
    @DisplayName("같은 문구도 책별로 독립 저장하고 목록 조회의 컬렉션 쿼리는 책 수에 비례해 늘지 않는다")
    void ownsSeparateMetadataAndLoadsBooksInBatch() {
        for (int index = 0; index < 3; index++) {
            bookRepository.save(new Book(new BookTitle("도서 " + index), null, null, 1, null, null, null,
                    List.of("비극", "로맨스"), List.of("첫사랑", "첫사랑")));
        }
        assertThat(jdbcTemplate.queryForList("SELECT id FROM book_genres WHERE value = ?", Long.class, "로맨스"))
                .hasSize(3).doesNotHaveDuplicates();
        assertThat(jdbcTemplate.queryForList("SELECT id FROM book_hashtags WHERE value = ?", Long.class, "첫사랑"))
                .hasSize(6).doesNotHaveDuplicates();
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        boolean wasEnabled = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        try {
            assertThat(adminBookService.findBooks().books()).hasSize(3).allSatisfy(response ->
                    assertThat(response).extracting(AdminBookResponse::genres, AdminBookResponse::hashtags)
                            .containsExactly(List.of("비극", "로맨스"), List.of("첫사랑", "첫사랑")));
            assertThat(statistics.getCollectionFetchCount()).isLessThanOrEqualTo(2);
        } finally {
            statistics.setStatisticsEnabled(wasEnabled);
            statistics.clear();
        }
    }
}
