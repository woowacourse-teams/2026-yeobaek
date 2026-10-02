package yeobaek.backend.application.appreciation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import yeobaek.backend.appreciation.api.CommentViewApi;
import yeobaek.backend.appreciation.domain.Comment;
import yeobaek.backend.application.appreciation.CommentQueryWorkflow.CommentQueryFailure;
import yeobaek.backend.foundation.identity.AppreciationId;
import yeobaek.backend.foundation.identity.ContentId;
import yeobaek.backend.foundation.identity.ContentLocationId;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.foundation.identity.SpaceId;
import yeobaek.backend.member.api.MemberQuery;
import yeobaek.backend.member.domain.MemberProfile;
import yeobaek.backend.readmodel.comment.CommentReadModel;
import yeobaek.backend.readmodel.comment.CommentReadModel.DiscoverySnapshot;
import yeobaek.backend.readmodel.comment.CommentReadModel.LocationSnapshot;
import yeobaek.backend.readmodel.comment.CommentReadModel.SpaceContentSnapshot;
import yeobaek.backend.space.api.SpaceAccessApi;
import yeobaek.backend.space.domain.Club;

class CommentQueryWorkflowTest {

    private static final MemberId REQUESTER = new MemberId(1L);
    private static final MemberId AUTHOR = new MemberId(2L);
    private static final SpaceId SPACE = new SpaceId(3L);
    private static final ContentId CONTENT = new ContentId(4L);
    private static final ContentLocationId LOCATION = new ContentLocationId(5L);
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 10, 2, 12, 0);

    @Test
    void returnsPureCommentsWithProfilesAndRecordsViews() {
        var comments = mock(CommentReadModel.class);
        var views = mock(CommentViewApi.class);
        var members = mock(MemberQuery.class);
        var spaces = mock(SpaceAccessApi.class);
        var workflow = new CommentQueryWorkflow(comments, views, members, spaces);
        var space = clubSnapshot(true);
        var location = new LocationSnapshot(LOCATION, CONTENT, 2);
        var comment = new Comment(new AppreciationId(6L), AUTHOR, "감상", CREATED_AT, null);
        given(spaces.canAccess(REQUESTER, SPACE)).willReturn(true);
        given(comments.findVisible(REQUESTER, SPACE, LOCATION)).willReturn(List.of(comment));
        given(members.findProfiles(Set.of(AUTHOR)))
                .willReturn(Map.of(AUTHOR, new MemberProfile(AUTHOR, "작성자")));

        var result = workflow.findComments(REQUESTER, space, location);

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.comment()).isSameAs(comment);
            assertThat(item.author()).isEqualTo(new MemberProfile(AUTHOR, "작성자"));
        });
        verify(views).markViewed(REQUESTER, List.of(new AppreciationId(6L)));
    }

    @Test
    void rejectsClubAccessBeforeResolvingTheSentence() {
        var comments = mock(CommentReadModel.class);
        var spaces = mock(SpaceAccessApi.class);
        var workflow = new CommentQueryWorkflow(
                comments, mock(CommentViewApi.class), mock(MemberQuery.class), spaces);
        given(comments.findClub(7L)).willReturn(Optional.of(clubSnapshot(true)));
        given(spaces.canAccess(REQUESTER, SPACE)).willReturn(false);

        assertThatThrownBy(() -> workflow.findClubComments(REQUESTER, 7L, 8L))
                .isInstanceOf(CommentQueryFailure.class)
                .extracting(failure -> ((CommentQueryFailure) failure).reason())
                .isEqualTo(CommentQueryWorkflow.FailureReason.NOT_CLUB_MEMBER);

        var ordered = inOrder(comments, spaces);
        ordered.verify(comments).findClub(7L);
        ordered.verify(spaces).canAccess(REQUESTER, SPACE);
        verify(comments, never()).findSentence(8L);
    }

    @Test
    void sortsDiscoveryByUnreadProgressThenLocation() {
        var comments = mock(CommentReadModel.class);
        var spaces = mock(SpaceAccessApi.class);
        var workflow = new CommentQueryWorkflow(
                comments, mock(CommentViewApi.class), mock(MemberQuery.class), spaces);
        var passage = new LocationSnapshot(LOCATION, CONTENT, 2);
        given(spaces.canAccess(REQUESTER, SPACE)).willReturn(true);
        given(comments.findDiscovery(REQUESTER, SPACE)).willReturn(List.of(
                discovery(11L, 1, 1, 0),
                discovery(12L, 3, 1, 1),
                discovery(13L, 2, 2, 1),
                discovery(14L, 2, 1, 1)));

        var result = workflow.findDiscovery(REQUESTER, clubSnapshot(true), passage);

        assertThat(result).extracting(CommentQueryWorkflow.DiscoveryResult::sentenceId)
                .containsExactly(14L, 13L, 12L, 11L);
        assertThat(result).extracting(CommentQueryWorkflow.DiscoveryResult::revealRequired)
                .containsExactly(false, false, true, false);
    }

    @Test
    void rejectsUnavailableContentBeforeRunningAQuery() {
        var comments = mock(CommentReadModel.class);
        var spaces = mock(SpaceAccessApi.class);
        var workflow = new CommentQueryWorkflow(
                comments, mock(CommentViewApi.class), mock(MemberQuery.class), spaces);
        given(spaces.canAccess(REQUESTER, SPACE)).willReturn(true);

        assertThatThrownBy(() -> workflow.countNewComments(
                REQUESTER, clubSnapshot(false), new LocationSnapshot(LOCATION, CONTENT, 2)))
                .isInstanceOf(CommentQueryFailure.class)
                .extracting(failure -> ((CommentQueryFailure) failure).reason())
                .isEqualTo(CommentQueryWorkflow.FailureReason.CONTENT_UNAVAILABLE);

        verify(comments, never()).countNewVisible(REQUESTER, SPACE, 2);
    }

    private SpaceContentSnapshot clubSnapshot(boolean available) {
        return new SpaceContentSnapshot(new Club(SPACE, 7L, "모임", "join"), CONTENT, available);
    }

    private DiscoverySnapshot discovery(Long sentenceId, int passageSequence,
                                        int sentenceSequence, long unreadCount) {
        return new DiscoverySnapshot(sentenceId, "문장", 20L + passageSequence, passageSequence,
                sentenceSequence, 1, unreadCount, CREATED_AT);
    }
}
