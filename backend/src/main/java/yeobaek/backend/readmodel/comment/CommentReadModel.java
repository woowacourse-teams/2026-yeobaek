package yeobaek.backend.readmodel.comment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import yeobaek.backend.appreciation.domain.Comment;
import yeobaek.backend.foundation.identity.ContentId;
import yeobaek.backend.foundation.identity.ContentLocationId;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.foundation.identity.SpaceId;
import yeobaek.backend.space.domain.Space;

public interface CommentReadModel {

    Optional<SpaceContentSnapshot> findClub(Long clubId);

    Optional<SpaceContentSnapshot> findPublicRoom(Long publicRoomId);

    Optional<LocationSnapshot> findSentence(Long sentenceId);

    Optional<LocationSnapshot> findPassage(Long passageId);

    List<Comment> findVisible(MemberId requesterId, SpaceId spaceId, ContentLocationId locationId);

    long countNewVisible(MemberId requesterId, SpaceId spaceId, int currentPassageSequence);

    List<DiscoverySnapshot> findDiscovery(MemberId requesterId, SpaceId spaceId);

    record SpaceContentSnapshot(Space space, ContentId contentId, boolean available) {
    }

    record LocationSnapshot(ContentLocationId locationId, ContentId contentId, int passageSequence) {
    }

    record DiscoverySnapshot(Long sentenceId, String content, Long passageId, int passageSequence,
                             int sentenceSequence, long commentCount, long unreadCommentCount,
                             LocalDateTime latestCommentCreatedAt) {
    }
}
