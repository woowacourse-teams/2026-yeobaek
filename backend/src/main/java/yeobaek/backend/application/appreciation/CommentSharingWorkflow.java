package yeobaek.backend.application.appreciation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.appreciation.api.CommentApi;
import yeobaek.backend.appreciation.domain.Comment;
import yeobaek.backend.collaboration.api.AppreciationContextApi;
import yeobaek.backend.content.api.ContentApi;
import yeobaek.backend.foundation.identity.ContentId;
import yeobaek.backend.foundation.identity.ContentLocationId;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.foundation.identity.SpaceId;
import yeobaek.backend.member.api.MemberQuery;
import yeobaek.backend.member.domain.MemberProfile;
import yeobaek.backend.collaboration.api.SpaceContentBindingApi;
import yeobaek.backend.space.api.SpaceAccessApi;

@Service
@RequiredArgsConstructor
public class CommentSharingWorkflow {

    private final CommentApi commentApi;
    private final AppreciationContextApi contextApi;
    private final ContentApi contentApi;
    private final MemberQuery memberQuery;
    private final SpaceAccessApi spaceAccessApi;
    private final SpaceContentBindingApi bindingApi;

    @Transactional
    public SharedComment share(MemberId authorId, SpaceId spaceId, ContentId contentId,
                               ContentLocationId locationId, String content) {
        MemberProfile author = memberQuery.getProfile(authorId);
        if (!spaceAccessApi.canAccess(authorId, spaceId)) {
            throw CommentPolicyFailure.spaceAccessDenied(spaceId);
        }
        if (!bindingApi.isBound(spaceId, contentId)) {
            throw CommentPolicyFailure.contentNotBound(spaceId, contentId);
        }
        if (!contentApi.ownsLocation(contentId, locationId)) {
            throw CommentPolicyFailure.locationNotInContent(contentId, locationId);
        }
        contentApi.requireAvailable(contentId);
        Comment comment = commentApi.create(authorId, content);
        contextApi.attach(comment.id(), spaceId, contentId, locationId);
        commentApi.markViewed(authorId, java.util.List.of(comment.id()));
        return new SharedComment(comment, author);
    }

    public record SharedComment(Comment comment, MemberProfile author) {
    }
}
