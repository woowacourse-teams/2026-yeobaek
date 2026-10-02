package yeobaek.backend.application.appreciation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.appreciation.api.CommentApi;
import yeobaek.backend.appreciation.api.CommentFailure;
import yeobaek.backend.application.appreciation.CommentSharingWorkflow.SharedComment;
import yeobaek.backend.collaboration.api.AppreciationContextApi;
import yeobaek.backend.content.api.ContentApi;
import yeobaek.backend.foundation.identity.AppreciationId;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.member.api.MemberQuery;
import yeobaek.backend.space.api.SpaceAccessApi;

@Service
@RequiredArgsConstructor
public class CommentModificationWorkflow {

    private final CommentApi commentApi;
    private final AppreciationContextApi contextApi;
    private final ContentApi contentApi;
    private final MemberQuery memberQuery;
    private final SpaceAccessApi spaceAccessApi;

    @Transactional
    public SharedComment update(MemberId actorId, AppreciationId commentId, String content) {
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

    private yeobaek.backend.appreciation.domain.Comment requireOwnedAccessibleComment(
            MemberId actorId, AppreciationId commentId) {
        var comment = commentApi.get(commentId);
        if (!comment.isWrittenBy(actorId)) {
            throw new CommentFailure(CommentFailure.Reason.NOT_OWNER, commentId);
        }
        var context = contextApi.get(commentId);
        if (!spaceAccessApi.canAccess(actorId, context.spaceId())) {
            throw CommentPolicyFailure.spaceAccessDenied(context.spaceId());
        }
        contentApi.requireAvailable(context.contentId());
        return comment;
    }
}
