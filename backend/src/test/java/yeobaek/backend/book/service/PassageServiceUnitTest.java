package yeobaek.backend.book.service;

import yeobaek.backend.web.compatibility.PassageService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import yeobaek.backend.club.domain.Club;
import yeobaek.backend.club.domain.vo.ClubName;
import yeobaek.backend.club.domain.vo.JoinCode;
import yeobaek.backend.club.repository.ClubRepository;
import yeobaek.backend.comment.internal.CommentQueryApiService;
import yeobaek.backend.collaboration.api.SpaceContentBindingApi;
import yeobaek.backend.comment.repository.CommentRepository;
import yeobaek.backend.content.api.ContentApi;
import yeobaek.backend.content.api.ContentBodyApi;
import yeobaek.backend.foundation.identity.ContentId;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.foundation.identity.SpaceId;
import yeobaek.backend.space.api.SpaceAccessApi;
import yeobaek.backend.member.api.MemberBlockApi;

@ExtendWith(MockitoExtension.class)
class PassageServiceUnitTest {

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private SpaceContentBindingApi bindingApi;

    @Mock
    private ContentApi contentApi;

    @Mock
    private ContentBodyApi contentBodyApi;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private SpaceAccessApi spaceAccessApi;

    @Mock
    private MemberBlockApi memberBlockApi;

    private PassageService passageService;

    @BeforeEach
    void setUp() {
        passageService = new PassageService(clubRepository, bindingApi, contentApi, contentBodyApi,
                new CommentQueryApiService(commentRepository), spaceAccessApi, memberBlockApi);
    }

    @Test
    @DisplayName("조회된 문장이 없으면 댓글 수 집계 쿼리를 실행하지 않는다")
    void skipCommentCountQueryWhenNoSentencesAreFound() {
        Club club = new Club(30L, new ClubName("모임"), new JoinCode("CODE01"));
        given(clubRepository.findById(10L)).willReturn(Optional.of(club));
        given(spaceAccessApi.canAccess(new MemberId(1L), new SpaceId(30L))).willReturn(true);
        ContentId contentId = new ContentId(20L);
        given(bindingApi.findContents(new SpaceId(30L))).willReturn(List.of(contentId));
        given(contentBodyApi.findPassages(contentId, 1, 10)).willReturn(List.of());

        var response = passageService.findPassages(1L, 10L, 1, 10);

        assertThat(response.passages()).isEmpty();
        verifyNoInteractions(commentRepository);
    }
}
