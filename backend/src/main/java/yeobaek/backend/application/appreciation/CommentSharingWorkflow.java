package yeobaek.backend.application.appreciation;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.appreciation.api.comment.CommentApi;
import yeobaek.backend.appreciation.api.comment.CommentContent;
import yeobaek.backend.appreciation.api.comment.CommentResponse;
import yeobaek.backend.appreciation.api.comment.CommentViewApi;
import yeobaek.backend.collaboration.api.context.AppreciationContextApi;
import yeobaek.backend.content.api.ContentApi;
import yeobaek.backend.content.api.location.ContentLocationQueryApi;
import yeobaek.backend.content.api.location.LocationKind;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.ContentLocationId;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.member.api.MemberQuery;
import yeobaek.backend.member.domain.MemberProfile;
import yeobaek.backend.collaboration.api.binding.SpaceContentBindingApi;
import yeobaek.backend.space.api.access.SpaceAccessApi;

@Service
@RequiredArgsConstructor
public class CommentSharingWorkflow {

    private final CommentApi commentApi;
    private final CommentViewApi commentViews;
    private final AppreciationContextApi contextApi;
    private final ContentApi contentApi;
    private final MemberQuery memberQuery;
    private final SpaceAccessApi spaceAccessApi;
    private final SpaceContentBindingApi bindingApi;
    private final ContentLocationQueryApi locations;

    @Transactional
    public SharedComment share(MemberId authorId, SpaceId spaceId, ContentId contentId,
                               ContentLocationId locationId, CommentContent content) {
        MemberProfile author = memberQuery.getProfile(authorId);
        if (!spaceAccessApi.canAccess(authorId, spaceId)) {
            throw new CommentPolicyException(ErrorCode.SPACE_ACCESS_DENIED,
                    "감상을 공유할 공간에 접근할 수 없습니다: spaceId=" + spaceId.value(),
                    Map.of("actorId", Long.toString(authorId.value()),
                            "spaceId", Long.toString(spaceId.value()),
                            "contentId", Long.toString(contentId.value()),
                            "locationId", Long.toString(locationId.value()),
                            "reason", "SPACE_ACCESS_DENIED"));
        }
        if (!bindingApi.isBound(spaceId, contentId)) {
            throw new CommentPolicyException(ErrorCode.CONTENT_NOT_BOUND,
                    "감상을 공유할 공간에 연결되지 않은 컨텐츠입니다: spaceId=" + spaceId.value()
                            + ", contentId=" + contentId.value(),
                    Map.of("actorId", Long.toString(authorId.value()),
                            "spaceId", Long.toString(spaceId.value()),
                            "contentId", Long.toString(contentId.value()),
                            "locationId", Long.toString(locationId.value()),
                            "reason", "CONTENT_NOT_BOUND"));
        }
        var location = locations.get(locationId);
        if (!location.contentId().equals(contentId) || !LocationKind.SENTENCE.equals(location.kind())) {
            throw new CommentPolicyException(ErrorCode.INVALID_REQUEST,
                    "감상을 공유할 위치가 컨텐츠의 문장이 아닙니다: contentId=" + contentId.value()
                            + ", locationId=" + locationId.value(),
                    Map.of("actorId", Long.toString(authorId.value()),
                            "spaceId", Long.toString(spaceId.value()),
                            "contentId", Long.toString(contentId.value()),
                            "locationId", Long.toString(locationId.value()),
                            "actualLocationKind", location.kind().value(),
                            "reason", "LOCATION_NOT_IN_CONTENT"));
        }
        contentApi.requireAvailable(contentId);
        CommentResponse comment = commentApi.create(authorId, content);
        contextApi.attach(comment.id(), spaceId, contentId, locationId);
        commentViews.markViewed(authorId, java.util.List.of(comment.id()));
        return new SharedComment(comment, author);
    }

    public record SharedComment(CommentResponse comment, MemberProfile author) {
    }
}
