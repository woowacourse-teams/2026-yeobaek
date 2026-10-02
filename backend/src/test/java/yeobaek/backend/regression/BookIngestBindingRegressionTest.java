package yeobaek.backend.regression;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
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
import yeobaek.backend.collaboration.api.SpaceContentBindingApi;
import yeobaek.backend.collaboration.persistence.PublicRoomContentBindingRepository;
import yeobaek.backend.collaboration.persistence.SpaceContentBindingRepository;
import yeobaek.backend.content.persistence.ContentRootRepository;
import yeobaek.backend.foundation.identity.ContentId;
import yeobaek.backend.foundation.identity.SpaceId;
import yeobaek.backend.publicroom.repository.PublicRoomRepository;
import yeobaek.backend.space.persistence.SpaceRootRepository;
import yeobaek.backend.support.IntegrationTest;

class BookIngestBindingRegressionTest extends IntegrationTest {

    @Autowired
    private BookIngestService bookIngestService;

    @Autowired
    private PublicRoomRepository publicRoomRepository;

    @Autowired
    private SpaceContentBindingApi bindingApi;

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
    @DisplayName("도서 업로드 성공은 공개방과 canonical 컨텐츠 연결을 같은 결과로 남긴다")
    void persistPublicRoomAndCanonicalBinding() {
        var response = bookIngestService.upload(request());
        var room = publicRoomRepository.findAll().getFirst();
        Long contentId = jdbcTemplate.queryForObject(
                "select content_id from books where id = ?", Long.class, response.bookId());

        assertThat(List.of(
                publicRoomRepository.count(),
                spaceRepository.count(),
                contentRepository.count(),
                bindingRepository.count(),
                roomBindingRepository.count(),
                jdbcTemplate.queryForObject("select count(*) from content_locations", Long.class),
                bindingApi.isBound(new SpaceId(room.getSpaceId()), new ContentId(contentId))))
                .containsExactly(1L, 1L, 1L, 1L, 1L, 2L, true);
    }

    private BookUploadRequest request() {
        return new BookUploadRequest(new BookTitle("연결 도서"), null, null, null,
                List.of(new AuthorEntryRequest(null, new AuthorName("작가"), null)),
                List.of(new ChapterUploadRequest(new ChapterTitle("1장"),
                        List.of(new PassageUploadRequest(
                                List.of(new SentenceUploadRequest(new SentenceContent("본문"))))))));
    }
}
