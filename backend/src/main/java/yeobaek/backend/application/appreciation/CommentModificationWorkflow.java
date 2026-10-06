package yeobaek.backend.application.appreciation;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.appreciation.api.comment.CommentApi;
import yeobaek.backend.appreciation.api.comment.CommentException;
import yeobaek.backend.appreciation.api.comment.CommentContent;
import yeobaek.backend.application.appreciation.CommentSharingWorkflow.SharedComment;
import yeobaek.backend.collaboration.api.context.AppreciationContextApi;
import yeobaek.backend.content.api.ContentApi;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.shared.identity.AppreciationId;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.member.api.MemberQuery;
import yeobaek.backend.space.api.access.SpaceAccessApi;

@Service
@RequiredArgsConstructor
public class CommentModificationWorkflow {

    private final CommentApi commentApi;
    private final AppreciationContextApi contextApi;
    private final ContentApi contentApi;
    private final MemberQuery memberQuery;
    private final SpaceAccessApi spaceAccessApi;

    @Transactional
    public SharedComment update(MemberId actorId, AppreciationId commentId, CommentContent content) {
        var comment = requireOwnedAccessibleComment(actorId, commentId);
        var changed = commentApi.update(actorId, comment.id(), content);
        return new SharedComment(changed, memberQuery.getProfile(changed.authorId()));
    }

    @Transactional
    public void delete(MemberId actorId, AppreciationId commentId) {
        requireOwnedAccessibleComment(actorId, commentId);
        contextApi.detach(commentId);
        commentApi.delete(actorId, commentId);
    }

    private yeobaek.backend.appreciation.api.comment.CommentResponse requireOwnedAccessibleComment(
            MemberId actorId, AppreciationId commentId) {
        var comment = commentApi.get(commentId);
        if (!comment.isWrittenBy(actorId)) {
            throw new CommentException(ErrorCode.NOT_COMMENT_OWNER, commentId,
                    "댓글 작성자만 수정하거나 삭제할 수 있습니다: commentId=" + commentId.value(),
                    Map.of("actorId", Long.toString(actorId.value()), "reason", "NOT_OWNER"));
        }
        var context = contextApi.get(commentId);
        if (!spaceAccessApi.canAccess(actorId, context.spaceId())) {
            throw new CommentPolicyException(ErrorCode.SPACE_ACCESS_DENIED,
                    "댓글을 수정하거나 삭제할 공간에 접근할 수 없습니다: spaceId="
                            + context.spaceId().value(),
                    Map.of("actorId", Long.toString(actorId.value()),
                            "commentId", Long.toString(commentId.value()),
                            "spaceId", Long.toString(context.spaceId().value()),
                            "contentId", Long.toString(context.contentId().value()),
                            "reason", "SPACE_ACCESS_DENIED"));
        }
        contentApi.requireAvailable(context.contentId());
        return comment;
    }
}
