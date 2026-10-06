package yeobaek.backend.web.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import yeobaek.backend.application.content.ContentReadingQueryService;
import yeobaek.backend.collaboration.api.binding.SpaceContentBindingApi;
import yeobaek.backend.content.api.location.ContentLegacyLocationQueryApi;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.space.api.club.ClubApi;
import yeobaek.backend.space.api.club.ClubResponse;

@ExtendWith(MockitoExtension.class)
class PassageServiceUnitTest {

    @Mock
    private ClubApi clubApi;

    @Mock
    private ClubResponse club;

    @Mock
    private SpaceContentBindingApi bindingApi;

    @Mock
    private ContentReadingQueryService queries;

    @Mock
    private ContentLegacyLocationQueryApi locations;

    private PassageService passageService;

    @BeforeEach
    void setUp() {
        passageService = new PassageService(clubApi, bindingApi, queries, locations);
    }

    @Test
    @DisplayName("조회된 문장이 없으면 댓글 수 집계 쿼리를 실행하지 않는다")
    void skipCommentCountQueryWhenNoSentencesAreFound() {
        given(club.id()).willReturn(new SpaceId(30L));
        given(clubApi.findById(10L)).willReturn(Optional.of(club));
        ContentId contentId = new ContentId(20L);
        given(bindingApi.findContents(new SpaceId(30L))).willReturn(List.of(contentId));
        given(queries.findBody(new yeobaek.backend.shared.identity.MemberId(1L), new SpaceId(30L),
                contentId, 1, 10)).willReturn(new ContentReadingQueryService.BodyResult(List.of()));

        var response = passageService.findPassages(1L, 10L, 1, 10);

        assertThat(response.passages()).isEmpty();
    }
}
