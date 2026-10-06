package yeobaek.backend.application.appreciation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.appreciation.api.comment.CommentApi;
import yeobaek.backend.appreciation.api.comment.CommentException;
import yeobaek.backend.collaboration.api.context.AppreciationContextApi;
import yeobaek.backend.content.api.ContentApi;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.shared.identity.AppreciationId;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.member.api.block.MemberBlockApi;
import yeobaek.backend.member.api.MemberQuery;
import yeobaek.backend.space.api.access.SpaceAccessApi;

import java.util.Map;
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
            throw new CommentException(ErrorCode.CANNOT_REPORT_OWN_COMMENT, commentId,
                    "자신이 작성한 댓글은 신고할 수 없습니다: commentId=" + commentId.value(),
                    Map.of("actorId", Long.toString(reporterId.value()),
                            "reason", "CANNOT_REPORT_OWN_COMMENT"));
        }
        if (!memberBlockApi.findBlockedMemberIds(reporterId, Set.of(comment.authorId())).isEmpty()) {
            throw new CommentException(ErrorCode.COMMENT_NOT_FOUND, commentId,
                    "신고할 수 없는 댓글입니다: commentId=" + commentId.value(),
                    Map.of("actorId", Long.toString(reporterId.value()),
                            "authorId", Long.toString(comment.authorId().value()),
                            "reason", "NOT_VISIBLE"));
        }
        var context = contextApi.get(commentId);
        if (!spaceAccessApi.canAccess(reporterId, context.spaceId())) {
            throw new CommentPolicyException(ErrorCode.SPACE_ACCESS_DENIED,
                    "댓글을 신고할 공간에 접근할 수 없습니다: spaceId=" + context.spaceId().value(),
                    Map.of("actorId", Long.toString(reporterId.value()),
                            "commentId", Long.toString(commentId.value()),
                            "spaceId", Long.toString(context.spaceId().value()),
                            "contentId", Long.toString(context.contentId().value()),
                            "reason", "SPACE_ACCESS_DENIED"));
        }
        contentApi.requireAvailable(context.contentId());
        return commentApi.report(reporterId, commentId);
    }
}
