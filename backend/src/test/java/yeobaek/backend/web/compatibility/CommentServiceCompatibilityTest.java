package yeobaek.backend.web.compatibility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import yeobaek.backend.appreciation.api.CommentFailure;
import yeobaek.backend.appreciation.domain.Comment;
import yeobaek.backend.application.appreciation.CommentModificationWorkflow;
import yeobaek.backend.application.appreciation.CommentQueryWorkflow;
import yeobaek.backend.application.appreciation.CommentQueryWorkflow.CommentTarget;
import yeobaek.backend.application.appreciation.CommentReportWorkflow;
import yeobaek.backend.application.appreciation.CommentSharingWorkflow;
import yeobaek.backend.application.appreciation.CommentSharingWorkflow.SharedComment;
import yeobaek.backend.comment.domain.vo.CommentContent;
import yeobaek.backend.foundation.identity.AppreciationId;
import yeobaek.backend.foundation.identity.ContentId;
import yeobaek.backend.foundation.identity.ContentLocationId;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.foundation.identity.SpaceId;
import yeobaek.backend.member.domain.MemberProfile;
import yeobaek.backend.support.BadRequestException;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.ForbiddenException;

class CommentServiceCompatibilityTest {

    private static final MemberId ACTOR = new MemberId(1L);
    private static final AppreciationId COMMENT = new AppreciationId(2L);

    @Test
    void createsThroughResolvedCanonicalTarget() {
        var sharing = mock(CommentSharingWorkflow.class);
        var queries = mock(CommentQueryWorkflow.class);
        var service = service(sharing, mock(CommentReportWorkflow.class),
                mock(CommentModificationWorkflow.class), queries);
        var target = new CommentTarget(new SpaceId(3L), new ContentId(4L), new ContentLocationId(5L));
        var comment = new Comment(COMMENT, ACTOR, "감상", LocalDateTime.of(2026, 10, 2, 12, 0), null);
        given(queries.resolveClubCommentTarget(ACTOR, 6L, 7L)).willReturn(target);
        given(sharing.share(ACTOR, target.spaceId(), target.contentId(), target.locationId(), "감상"))
                .willReturn(new SharedComment(comment, new MemberProfile(ACTOR, "독자")));

        var response = service.create(ACTOR.value(), 6L, 7L, new CommentContent("감상"));

        assertThat(response.commentId()).isEqualTo(COMMENT.value());
        assertThat(response.mine()).isTrue();
        verify(sharing).share(ACTOR, target.spaceId(), target.contentId(), target.locationId(), "감상");
    }

    @Test
    void mapsCommandOwnershipFailureToV1ForbiddenError() {
        var modification = mock(CommentModificationWorkflow.class);
        var service = service(mock(CommentSharingWorkflow.class), mock(CommentReportWorkflow.class),
                modification, mock(CommentQueryWorkflow.class));
        given(modification.update(ACTOR, COMMENT, "수정"))
                .willThrow(new CommentFailure(CommentFailure.Reason.NOT_OWNER, COMMENT));

        assertThatThrownBy(() -> service.update(ACTOR.value(), COMMENT.value(), new CommentContent("수정")))
                .isInstanceOf(ForbiddenException.class)
                .extracting("code").isEqualTo(ErrorCode.NOT_COMMENT_OWNER);
    }

    @Test
    void mapsOwnCommentReportFailureToV1BadRequestError() {
        var reports = mock(CommentReportWorkflow.class);
        var service = service(mock(CommentSharingWorkflow.class), reports,
                mock(CommentModificationWorkflow.class), mock(CommentQueryWorkflow.class));
        given(reports.report(ACTOR, COMMENT))
                .willThrow(new CommentFailure(CommentFailure.Reason.CANNOT_REPORT_OWN_COMMENT, COMMENT));

        assertThatThrownBy(() -> service.report(ACTOR.value(), COMMENT.value()))
                .isInstanceOf(BadRequestException.class)
                .extracting("code").isEqualTo(ErrorCode.CANNOT_REPORT_OWN_COMMENT);
    }

    private CommentService service(CommentSharingWorkflow sharing, CommentReportWorkflow reports,
                                   CommentModificationWorkflow modification,
                                   CommentQueryWorkflow queries) {
        return new CommentService(sharing, reports, modification, queries);
    }
}
