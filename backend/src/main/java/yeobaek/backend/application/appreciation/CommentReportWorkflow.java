package yeobaek.backend.application.appreciation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.appreciation.api.CommentApi;
import yeobaek.backend.appreciation.api.CommentFailure;
import yeobaek.backend.collaboration.api.AppreciationContextApi;
import yeobaek.backend.content.api.ContentApi;
import yeobaek.backend.foundation.identity.AppreciationId;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.member.api.MemberBlockApi;
import yeobaek.backend.member.api.MemberQuery;
import yeobaek.backend.space.api.SpaceAccessApi;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class CommentReportWorkflow {

    private final CommentApi commentApi;
    private final AppreciationContextApi contextApi;
    private final ContentApi contentApi;
    private final MemberQuery memberQuery;
    private final MemberBlockApi memberBlockApi;
    private final SpaceAccessApi spaceAccessApi;

    @Transactional
    public boolean report(MemberId reporterId, AppreciationId commentId) {
        memberQuery.getProfile(reporterId);
        var comment = commentApi.getForUpdate(commentId);
        if (comment.isWrittenBy(reporterId)) {
            throw new CommentFailure(CommentFailure.Reason.CANNOT_REPORT_OWN_COMMENT, commentId);
        }
        if (!memberBlockApi.findBlockedMemberIds(reporterId, Set.of(comment.authorId())).isEmpty()) {
            throw new CommentFailure(CommentFailure.Reason.NOT_VISIBLE, commentId);
        }
        var context = contextApi.get(commentId);
        if (!spaceAccessApi.canAccess(reporterId, context.spaceId())) {
            throw CommentPolicyFailure.spaceAccessDenied(context.spaceId());
        }
        contentApi.requireAvailable(context.contentId());
        return commentApi.report(reporterId, commentId);
    }
}
