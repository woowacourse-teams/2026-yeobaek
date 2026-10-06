package yeobaek.backend.web.v1;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import yeobaek.backend.appreciation.api.comment.CommentException;
import yeobaek.backend.application.appreciation.CommentModificationWorkflow;
import yeobaek.backend.application.appreciation.CommentQueryWorkflow;
import yeobaek.backend.application.appreciation.CommentReportWorkflow;
import yeobaek.backend.application.appreciation.CommentSharingWorkflow;
import yeobaek.backend.appreciation.api.comment.CommentContent;
import yeobaek.backend.shared.identity.AppreciationId;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.space.api.access.SpaceAccessApi;
import yeobaek.backend.space.api.club.ClubApi;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.shared.exception.ForbiddenException;

class CommentServiceCompatibilityTest {

    private static final MemberId ACTOR = new MemberId(1L);
    private static final AppreciationId COMMENT = new AppreciationId(2L);

    @Test
    void propagatesCommandOwnershipFailureWithoutRemapping() {
        var modification = mock(CommentModificationWorkflow.class);
        var service = service(mock(CommentSharingWorkflow.class), mock(CommentReportWorkflow.class),
                modification, mock(CommentQueryWorkflow.class));
        CommentContent changed = new CommentContent("수정");
        var cause = new IllegalStateException("ownership lookup failed");
        var failure = new CommentException(ErrorCode.NOT_COMMENT_OWNER, COMMENT,
                "댓글 작성자만 댓글을 변경할 수 있습니다: commentId=" + COMMENT.value(),
                java.util.Map.of("reason", "NOT_OWNER"), cause);
        given(modification.update(ACTOR, COMMENT, changed)).willThrow(failure);

        assertThatThrownBy(() -> service.update(ACTOR.value(), COMMENT.value(), new CommentContent("수정")))
                .isSameAs(failure)
                .hasMessage("댓글 작성자만 댓글을 변경할 수 있습니다: commentId=2")
                .hasCause(cause)
                .extracting("code").isEqualTo(ErrorCode.NOT_COMMENT_OWNER);
    }

    @Test
    void propagatesOwnCommentReportFailureWithoutRemapping() {
        var reports = mock(CommentReportWorkflow.class);
        var service = service(mock(CommentSharingWorkflow.class), reports,
                mock(CommentModificationWorkflow.class), mock(CommentQueryWorkflow.class));
        var cause = new IllegalStateException("report policy failed");
        var failure = new CommentException(ErrorCode.CANNOT_REPORT_OWN_COMMENT, COMMENT,
                "자신이 작성한 댓글은 신고할 수 없습니다: commentId=" + COMMENT.value(),
                java.util.Map.of("reason", "CANNOT_REPORT_OWN_COMMENT"), cause);
        given(reports.report(ACTOR, COMMENT)).willThrow(failure);

        assertThatThrownBy(() -> service.report(ACTOR.value(), COMMENT.value()))
                .isSameAs(failure)
                .hasMessage("자신이 작성한 댓글은 신고할 수 없습니다: commentId=2")
                .hasCause(cause)
                .extracting("code").isEqualTo(ErrorCode.CANNOT_REPORT_OWN_COMMENT);
    }

    @Test
    void deniesLegacyLocationLookupBeforeResolvingPrivateTarget() {
        var clubs = mock(ClubApi.class);
        var spaces = mock(SpaceAccessApi.class);
        var bindings = mock(yeobaek.backend.collaboration.api.binding.SpaceContentBindingApi.class);
        var locations = mock(yeobaek.backend.content.api.location.ContentLegacyLocationQueryApi.class);
        var club = mock(yeobaek.backend.space.api.club.ClubResponse.class);
        var spaceId = new SpaceId(3L);
        given(club.id()).willReturn(spaceId);
        given(clubs.findById(4L)).willReturn(java.util.Optional.of(club));
        given(spaces.canAccess(ACTOR, spaceId)).willReturn(false);
        var service = new CommentService(mock(CommentSharingWorkflow.class), mock(CommentReportWorkflow.class),
                mock(CommentModificationWorkflow.class), mock(CommentQueryWorkflow.class), clubs,
                mock(yeobaek.backend.space.api.publicroom.PublicRoomApi.class), bindings, locations, spaces);

        assertThatThrownBy(() -> service.findComments(ACTOR.value(), 4L, 5L))
                .isInstanceOf(ForbiddenException.class)
                .extracting("code").isEqualTo(ErrorCode.SPACE_ACCESS_DENIED);
        verifyNoInteractions(bindings, locations);
    }

    private CommentService service(CommentSharingWorkflow sharing, CommentReportWorkflow reports,
                                   CommentModificationWorkflow modification,
                                   CommentQueryWorkflow queries) {
        return new CommentService(sharing, reports, modification, queries,
                mock(yeobaek.backend.space.api.club.ClubApi.class),
                mock(yeobaek.backend.space.api.publicroom.PublicRoomApi.class),
                mock(yeobaek.backend.collaboration.api.binding.SpaceContentBindingApi.class),
                mock(yeobaek.backend.content.api.location.ContentLegacyLocationQueryApi.class),
                mock(SpaceAccessApi.class));
    }
}
