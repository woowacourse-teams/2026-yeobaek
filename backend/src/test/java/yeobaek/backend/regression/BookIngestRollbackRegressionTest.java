package yeobaek.backend.regression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import yeobaek.backend.admin.dto.AuthorEntryRequest;
import yeobaek.backend.admin.dto.BookUploadRequest;
import yeobaek.backend.admin.dto.ChapterUploadRequest;
import yeobaek.backend.admin.dto.PassageUploadRequest;
import yeobaek.backend.admin.dto.SentenceUploadRequest;
import yeobaek.backend.admin.service.BookIngestService;
import yeobaek.backend.book.domain.vo.AuthorName;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.book.domain.vo.ChapterTitle;
import yeobaek.backend.book.domain.vo.SentenceContent;
import yeobaek.backend.book.repository.AuthorRepository;
import yeobaek.backend.book.repository.BookManagementRepository;
import yeobaek.backend.book.repository.ChapterRepository;
import yeobaek.backend.book.repository.PassageRepository;
import yeobaek.backend.book.service.BookCoverUrlResolver;
import yeobaek.backend.collaboration.persistence.PublicRoomContentBindingRepository;
import yeobaek.backend.collaboration.persistence.SpaceContentBindingRepository;
import yeobaek.backend.content.persistence.ContentRootRepository;
import yeobaek.backend.publicroom.repository.PublicRoomRepository;
import yeobaek.backend.space.persistence.SpaceRootRepository;
import yeobaek.backend.support.IntegrationTest;

class BookIngestRollbackRegressionTest extends IntegrationTest {

    @Autowired
    private BookIngestService bookIngestService;

    @MockitoBean
    private BookCoverUrlResolver coverUrlResolver;

    @Autowired
    private BookManagementRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private PassageRepository passageRepository;

    @Autowired
    private PublicRoomRepository publicRoomRepository;

    @Autowired
    private SpaceContentBindingRepository bindingRepository;

    @Autowired
    private PublicRoomContentBindingRepository roomBindingRepository;

    @Autowired
    private SpaceRootRepository spaceRepository;

    @Autowired
    private ContentRootRepository contentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("공개방과 본문 연결 이후의 업로드 실패는 전체 canonical 그래프를 롤백한다")
    void rollbackEveryPersistedUploadComponentAfterLateFailure() {
        given(coverUrlResolver.resolve(null)).willThrow(new IllegalStateException("response assembly failed"));

        assertThatThrownBy(() -> bookIngestService.upload(request()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("response assembly failed");
        assertThat(List.of(
                bookRepository.count(),
                authorRepository.count(),
                chapterRepository.count(),
                passageRepository.count(),
                publicRoomRepository.count(),
                contentRepository.count(),
                spaceRepository.count(),
                bindingRepository.count(),
                roomBindingRepository.count(),
                jdbcTemplate.queryForObject("select count(*) from content_locations", Long.class)))
                .containsOnly(0L);
    }

    private BookUploadRequest request() {
        return new BookUploadRequest(new BookTitle("롤백 도서"), null, null, null,
                List.of(new AuthorEntryRequest(null, new AuthorName("롤백 작가"), null)),
                List.of(new ChapterUploadRequest(new ChapterTitle("1장"),
                        List.of(new PassageUploadRequest(
                                List.of(new SentenceUploadRequest(new SentenceContent("본문"))))))));
    }
}
