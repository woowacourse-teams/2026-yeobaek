package yeobaek.backend.readmodel.comment;

import java.time.LocalDateTime;
import java.util.List;
import yeobaek.backend.appreciation.api.comment.CommentResponse;
import yeobaek.backend.shared.identity.ContentLocationId;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;

public interface CommentReadModel {

    List<CommentResponse> findVisible(MemberId requesterId, SpaceId spaceId, ContentLocationId locationId);

    long countNewVisible(MemberId requesterId, SpaceId spaceId, ContentId contentId, int currentPassageSequence);

    List<DiscoverySnapshot> findDiscovery(MemberId requesterId, SpaceId spaceId, ContentId contentId);

    record DiscoverySnapshot(ContentLocationId locationId, ContentLocationId passageLocationId,
                             long legacySentenceId, String content, int passageSequence,
                             int sentenceSequence, long commentCount, long unreadCommentCount,
                             LocalDateTime latestCommentCreatedAt) {
    }
}
