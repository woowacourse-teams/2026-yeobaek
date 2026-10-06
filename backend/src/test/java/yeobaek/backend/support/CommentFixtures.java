package yeobaek.backend.support;

import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import yeobaek.backend.content.book.domain.Sentence;
import yeobaek.backend.space.club.domain.ClubMember;
import yeobaek.backend.appreciation.comment.persistence.Comment;
import yeobaek.backend.appreciation.api.comment.CommentContent;
import yeobaek.backend.collaboration.persistence.AppreciationContextJpaEntity;
import yeobaek.backend.shared.identity.AppreciationId;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.space.publicroom.persistence.PublicRoom;

public final class CommentFixtures {

    private static final Map<Long, Target> TARGETS = Collections.synchronizedMap(new HashMap<>());

    private CommentFixtures() {
    }

    public static void clear() {
        TARGETS.clear();
    }

    public static Comment inClub(AppreciationId appreciationId, ContentId contentId, ClubMember membership,
                                 Sentence sentence, String content) {
        return inClub(appreciationId, contentId, membership, sentence, new CommentContent(content));
    }

    public static Comment inClub(AppreciationId appreciationId, ContentId contentId, ClubMember membership,
                                 Sentence sentence, CommentContent content) {
        return targeted(new Comment(appreciationId.value(), content), membership.getClub().getSpaceId(),
                contentId, sentence);
    }

    public static Comment inPublicRoom(AppreciationId appreciationId, ContentId contentId, PublicRoom room,
                                       Sentence sentence, String content) {
        return inPublicRoom(appreciationId, contentId, room, sentence, new CommentContent(content));
    }

    public static Comment inPublicRoom(AppreciationId appreciationId, ContentId contentId, PublicRoom room,
                                       Sentence sentence, CommentContent content) {
        return targeted(new Comment(appreciationId.value(), content), room.getSpaceId(), contentId, sentence);
    }

    public static AppreciationContextJpaEntity contextOf(Comment comment) {
        Target target = TARGETS.get(comment.getId());
        if (target == null || comment.getId() == null) {
            throw new IllegalStateException("저장된 댓글 fixture의 대상 정보가 필요합니다.");
        }
        return new AppreciationContextJpaEntity(comment.getId(), target.spaceId(), target.contentId(),
                target.locationId());
    }

    private static Comment targeted(Comment comment, Long spaceId, ContentId contentId, Sentence sentence) {
        TARGETS.put(comment.getId(), new Target(spaceId, contentId.value(),
                sentence.getLocationId()));
        return comment;
    }

    private record Target(Long spaceId, Long contentId, Long locationId) {
    }
}
