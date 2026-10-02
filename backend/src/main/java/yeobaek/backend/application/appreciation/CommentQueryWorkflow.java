package yeobaek.backend.application.appreciation;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.appreciation.api.CommentViewApi;
import yeobaek.backend.appreciation.domain.Comment;
import yeobaek.backend.foundation.identity.ContentId;
import yeobaek.backend.foundation.identity.ContentLocationId;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.foundation.identity.SpaceId;
import yeobaek.backend.member.api.MemberQuery;
import yeobaek.backend.member.domain.MemberProfile;
import yeobaek.backend.readmodel.comment.CommentReadModel;
import yeobaek.backend.readmodel.comment.CommentReadModel.DiscoverySnapshot;
import yeobaek.backend.readmodel.comment.CommentReadModel.LocationSnapshot;
import yeobaek.backend.readmodel.comment.CommentReadModel.SpaceContentSnapshot;
import yeobaek.backend.space.api.SpaceAccessApi;

@Service
@RequiredArgsConstructor
public class CommentQueryWorkflow {

    private static final String CLUB_ID = "clubId";
    private static final String SENTENCE_ID = "sentenceId";
    private static final String PASSAGE_ID = "passageId";
    private static final String PASSAGE_ID_SUFFIX = ", passageId=";

    private final CommentReadModel comments;
    private final CommentViewApi views;
    private final MemberQuery members;
    private final SpaceAccessApi spaces;

    @Transactional
    public List<CommentResult> findClubComments(MemberId requesterId, Long clubId, Long sentenceId) {
        ResolvedTarget target = resolveClubTarget(requesterId, clubId, sentenceId);
        return loadComments(requesterId, target.space(), target.location());
    }

    @Transactional
    public List<CommentResult> findPublicRoomComments(MemberId requesterId, Long publicRoomId, Long sentenceId) {
        ResolvedTarget target = resolvePublicRoomTarget(publicRoomId, sentenceId);
        return loadComments(requesterId, target.space(), target.location());
    }

    @Transactional(readOnly = true)
    public CommentTarget resolveClubCommentTarget(MemberId requesterId, Long clubId, Long sentenceId) {
        return resolveClubTarget(requesterId, clubId, sentenceId).toCommentTarget();
    }

    @Transactional(readOnly = true)
    public CommentTarget resolvePublicRoomCommentTarget(Long publicRoomId, Long sentenceId) {
        return resolvePublicRoomTarget(publicRoomId, sentenceId).toCommentTarget();
    }

    @Transactional(readOnly = true)
    public long countNewClubComments(MemberId requesterId, Long clubId, Long passageId) {
        SpaceContentSnapshot space = requireClubForDiscovery(clubId, passageId);
        requireAccess(requesterId, space,
                "모임에 참여 중인 회원만 댓글 발견 정보를 조회할 수 있습니다: clubId=" + clubId,
                passageContext(clubId, passageId));
        LocationSnapshot passage = requirePassage(passageId, passageContext(clubId, passageId));
        requirePassageOwnership(space, passage, "현재 문단이 해당 모임의 도서에 속하지 않습니다: clubId="
                + clubId + PASSAGE_ID_SUFFIX + passageId, passageContext(clubId, passageId));
        return loadNewCommentCount(requesterId, space, passage);
    }

    @Transactional(readOnly = true)
    public long countNewPublicRoomComments(MemberId requesterId, Long publicRoomId, Long passageId) {
        SpaceContentSnapshot space = requirePublicRoom(publicRoomId);
        LocationSnapshot passage = requirePassage(passageId, Map.of());
        requirePassageOwnership(space, passage,
                "현재 문단이 해당 공개방의 도서에 속하지 않습니다: publicRoomId=" + publicRoomId
                        + PASSAGE_ID_SUFFIX + passageId, Map.of());
        return loadNewCommentCount(requesterId, space, passage);
    }

    @Transactional(readOnly = true)
    public List<DiscoveryResult> findClubDiscovery(MemberId requesterId, Long clubId, Long passageId) {
        SpaceContentSnapshot space = requireClubForDiscovery(clubId, passageId);
        requireAccess(requesterId, space,
                "모임에 참여 중인 회원만 댓글 발견 정보를 조회할 수 있습니다: clubId=" + clubId,
                passageContext(clubId, passageId));
        LocationSnapshot passage = requirePassage(passageId, passageContext(clubId, passageId));
        requirePassageOwnership(space, passage, "현재 문단이 해당 모임의 도서에 속하지 않습니다: clubId="
                + clubId + PASSAGE_ID_SUFFIX + passageId, passageContext(clubId, passageId));
        return loadDiscovery(requesterId, space, passage);
    }

    @Transactional(readOnly = true)
    public List<DiscoveryResult> findPublicRoomDiscovery(MemberId requesterId, Long publicRoomId, Long passageId) {
        SpaceContentSnapshot space = requirePublicRoom(publicRoomId);
        LocationSnapshot passage = requirePassage(passageId, Map.of());
        requirePassageOwnership(space, passage,
                "현재 문단이 해당 공개방의 도서에 속하지 않습니다: publicRoomId=" + publicRoomId
                        + PASSAGE_ID_SUFFIX + passageId, Map.of());
        return loadDiscovery(requesterId, space, passage);
    }

    public List<CommentResult> findComments(MemberId requesterId, SpaceContentSnapshot space,
                                            LocationSnapshot location) {
        requireGenericAccess(requesterId, space,
                "공간에 접근할 수 있는 회원만 댓글을 조회할 수 있습니다: spaceId=" + space.space().id().value(),
                Map.of());
        requireGenericOwnership(space, location);
        requireGenericAvailability(space);
        return loadComments(requesterId, space, location);
    }

    private List<CommentResult> loadComments(MemberId requesterId, SpaceContentSnapshot space,
                                             LocationSnapshot location) {
        List<Comment> visible = comments.findVisible(requesterId, space.space().id(), location.locationId());
        Map<MemberId, MemberProfile> profiles = profiles(visible);
        List<CommentResult> results = visible.stream()
                .map(comment -> new CommentResult(comment, profiles.get(comment.authorId())))
                .toList();
        views.markViewed(requesterId, visible.stream().map(Comment::id).toList());
        return results;
    }

    public long countNewComments(MemberId requesterId, SpaceContentSnapshot space, LocationSnapshot passage) {
        requireGenericAccess(requesterId, space,
                "공간에 접근할 수 있는 회원만 댓글 발견 정보를 조회할 수 있습니다: spaceId="
                        + space.space().id().value(), Map.of());
        requireGenericOwnership(space, passage);
        requireGenericAvailability(space);
        return loadNewCommentCount(requesterId, space, passage);
    }

    private long loadNewCommentCount(MemberId requesterId, SpaceContentSnapshot space,
                                     LocationSnapshot passage) {
        return comments.countNewVisible(requesterId, space.space().id(), passage.passageSequence());
    }

    public List<DiscoveryResult> findDiscovery(MemberId requesterId, SpaceContentSnapshot space,
                                               LocationSnapshot passage) {
        requireGenericAccess(requesterId, space,
                "공간에 접근할 수 있는 회원만 댓글 발견 정보를 조회할 수 있습니다: spaceId="
                        + space.space().id().value(), Map.of());
        requireGenericOwnership(space, passage);
        requireGenericAvailability(space);
        return loadDiscovery(requesterId, space, passage);
    }

    private List<DiscoveryResult> loadDiscovery(MemberId requesterId, SpaceContentSnapshot space,
                                                LocationSnapshot passage) {
        return comments.findDiscovery(requesterId, space.space().id()).stream()
                .map(snapshot -> toDiscovery(snapshot, passage.passageSequence()))
                .sorted(discoveryComparator())
                .toList();
    }

    private SpaceContentSnapshot requireClub(Long clubId, Long sentenceId) {
        return comments.findClub(clubId).orElseThrow(() -> new CommentQueryFailure(
                FailureReason.CLUB_NOT_FOUND,
                "댓글을 조회하거나 작성할 모임이 존재하지 않습니다: clubId=" + clubId,
                Map.of(CLUB_ID, clubId.toString(), SENTENCE_ID, sentenceId.toString())));
    }

    private ResolvedTarget resolveClubTarget(MemberId requesterId, Long clubId, Long sentenceId) {
        SpaceContentSnapshot space = requireClub(clubId, sentenceId);
        requireAccess(requesterId, space,
                "모임에 참여 중인 회원만 댓글을 조회하거나 작성할 수 있습니다: clubId=" + clubId,
                Map.of(CLUB_ID, clubId.toString(), SENTENCE_ID, sentenceId.toString()));
        LocationSnapshot sentence = requireClubSentence(clubId, sentenceId);
        requireClubSentenceOwnership(space, sentence, clubId, sentenceId);
        return new ResolvedTarget(space, sentence);
    }

    private ResolvedTarget resolvePublicRoomTarget(Long publicRoomId, Long sentenceId) {
        SpaceContentSnapshot space = requirePublicRoom(publicRoomId);
        LocationSnapshot sentence = requirePublicRoomSentence(sentenceId);
        requirePublicRoomSentenceOwnership(space, sentence, publicRoomId, sentenceId);
        return new ResolvedTarget(space, sentence);
    }

    private SpaceContentSnapshot requireClubForDiscovery(Long clubId, Long passageId) {
        return comments.findClub(clubId).orElseThrow(() -> new CommentQueryFailure(
                FailureReason.CLUB_NOT_FOUND,
                "댓글 발견 정보를 조회할 모임이 존재하지 않습니다: clubId=" + clubId,
                passageContext(clubId, passageId)));
    }

    private SpaceContentSnapshot requirePublicRoom(Long publicRoomId) {
        return comments.findPublicRoom(publicRoomId).orElseThrow(() -> new CommentQueryFailure(
                FailureReason.PUBLIC_ROOM_NOT_FOUND,
                "공개방이 존재하지 않습니다: publicRoomId=" + publicRoomId, Map.of()));
    }

    private LocationSnapshot requireClubSentence(Long clubId, Long sentenceId) {
        return comments.findSentence(sentenceId).orElseThrow(() -> new CommentQueryFailure(
                FailureReason.SENTENCE_NOT_FOUND,
                "댓글을 조회하거나 작성할 문장이 존재하지 않습니다: sentenceId=" + sentenceId,
                Map.of(CLUB_ID, clubId.toString(), SENTENCE_ID, sentenceId.toString())));
    }

    private LocationSnapshot requirePublicRoomSentence(Long sentenceId) {
        return comments.findSentence(sentenceId).orElseThrow(() -> new CommentQueryFailure(
                FailureReason.SENTENCE_NOT_FOUND,
                "댓글을 조회하거나 작성할 문장이 존재하지 않습니다: sentenceId=" + sentenceId, Map.of()));
    }

    private LocationSnapshot requirePassage(Long passageId, Map<String, String> context) {
        return comments.findPassage(passageId).orElseThrow(() -> new CommentQueryFailure(
                FailureReason.INVALID_REQUEST, "현재 문단이 존재하지 않습니다: passageId=" + passageId, context));
    }

    private void requireClubSentenceOwnership(SpaceContentSnapshot space, LocationSnapshot sentence,
                                              Long clubId, Long sentenceId) {
        if (!space.contentId().equals(sentence.contentId())) {
            throw new CommentQueryFailure(FailureReason.SENTENCE_NOT_IN_CLUB_CONTENT,
                    "해당 모임에서 읽는 문장이 아닙니다: clubId=" + clubId + ", sentenceId=" + sentenceId,
                    Map.of(CLUB_ID, clubId.toString(), SENTENCE_ID, sentenceId.toString()));
        }
        requireAvailable(space, Map.of(CLUB_ID, clubId.toString(), SENTENCE_ID, sentenceId.toString()));
    }

    private void requirePublicRoomSentenceOwnership(SpaceContentSnapshot space, LocationSnapshot sentence,
                                                    Long publicRoomId, Long sentenceId) {
        if (!space.contentId().equals(sentence.contentId())) {
            throw new CommentQueryFailure(FailureReason.SENTENCE_NOT_FOUND,
                    "해당 공개방에서 읽는 문장이 아닙니다: publicRoomId=" + publicRoomId
                            + ", sentenceId=" + sentenceId, Map.of());
        }
        requireAvailable(space, Map.of());
    }

    private void requirePassageOwnership(SpaceContentSnapshot space, LocationSnapshot passage,
                                         String message, Map<String, String> context) {
        if (!space.contentId().equals(passage.contentId())) {
            throw new CommentQueryFailure(FailureReason.INVALID_REQUEST, message, context);
        }
        requireAvailable(space, context);
    }

    private void requireAccess(MemberId requesterId, SpaceContentSnapshot space, String message,
                               Map<String, String> context) {
        if (!spaces.canAccess(requesterId, space.space().id())) {
            throw new CommentQueryFailure(FailureReason.NOT_CLUB_MEMBER, message, context);
        }
    }

    private void requireGenericAccess(MemberId requesterId, SpaceContentSnapshot space, String message,
                                      Map<String, String> context) {
        if (!spaces.canAccess(requesterId, space.space().id())) {
            throw new CommentQueryFailure(FailureReason.SPACE_ACCESS_DENIED, message, context);
        }
    }

    private void requireGenericOwnership(SpaceContentSnapshot space, LocationSnapshot location) {
        if (!space.contentId().equals(location.contentId())) {
            throw new CommentQueryFailure(FailureReason.INVALID_REQUEST,
                    "컨텐츠에 속하지 않는 위치입니다: locationId=" + location.locationId().value(), Map.of());
        }
    }

    private void requireAvailable(SpaceContentSnapshot space, Map<String, String> context) {
        if (!space.available()) {
            throw new CommentQueryFailure(FailureReason.BOOK_NOT_AVAILABLE,
                    "더 이상 이용할 수 없는 도서입니다.", context);
        }
    }

    private void requireGenericAvailability(SpaceContentSnapshot space) {
        if (!space.available()) {
            throw new CommentQueryFailure(FailureReason.CONTENT_UNAVAILABLE,
                    "더 이상 이용할 수 없는 컨텐츠입니다.", Map.of());
        }
    }

    private Map<MemberId, MemberProfile> profiles(List<Comment> visible) {
        if (visible.isEmpty()) {
            return Map.of();
        }
        Set<MemberId> authorIds = visible.stream().map(Comment::authorId).collect(Collectors.toSet());
        return members.findProfiles(authorIds);
    }

    private DiscoveryResult toDiscovery(DiscoverySnapshot snapshot, int currentPassageSequence) {
        boolean future = snapshot.passageSequence() > currentPassageSequence;
        return new DiscoveryResult(snapshot.sentenceId(), snapshot.content(), snapshot.passageId(),
                snapshot.passageSequence(), snapshot.sentenceSequence(), future, snapshot.commentCount(),
                snapshot.unreadCommentCount(), future && snapshot.unreadCommentCount() > 0,
                snapshot.latestCommentCreatedAt());
    }

    private Comparator<DiscoveryResult> discoveryComparator() {
        return Comparator.comparingInt(this::sortGroup)
                .thenComparingInt(DiscoveryResult::passageSequence)
                .thenComparingInt(DiscoveryResult::sentenceSequence)
                .thenComparing(DiscoveryResult::sentenceId, Comparator.reverseOrder());
    }

    private int sortGroup(DiscoveryResult result) {
        if (result.unreadCommentCount() == 0) {
            return 2;
        }
        return result.future() ? 1 : 0;
    }

    private Map<String, String> passageContext(Long clubId, Long passageId) {
        return Map.of(CLUB_ID, clubId.toString(), PASSAGE_ID, passageId.toString());
    }

    public record CommentResult(Comment comment, MemberProfile author) {
    }

    public record DiscoveryResult(Long sentenceId, String content, Long passageId, int passageSequence,
                                  int sentenceSequence, boolean future, long commentCount,
                                  long unreadCommentCount, boolean revealRequired,
                                  LocalDateTime latestCommentCreatedAt) {
    }

    public record CommentTarget(SpaceId spaceId, ContentId contentId, ContentLocationId locationId) {
    }

    private record ResolvedTarget(SpaceContentSnapshot space, LocationSnapshot location) {

        private CommentTarget toCommentTarget() {
            return new CommentTarget(space.space().id(), space.contentId(), location.locationId());
        }
    }

    public enum FailureReason {
        CLUB_NOT_FOUND,
        PUBLIC_ROOM_NOT_FOUND,
        NOT_CLUB_MEMBER,
        SENTENCE_NOT_FOUND,
        SENTENCE_NOT_IN_CLUB_CONTENT,
        INVALID_REQUEST,
        BOOK_NOT_AVAILABLE,
        SPACE_ACCESS_DENIED,
        CONTENT_UNAVAILABLE
    }

    public static final class CommentQueryFailure extends RuntimeException {

        private final transient FailureReason failureReason;
        private final transient Map<String, String> logContext;

        public CommentQueryFailure(FailureReason reason, String message, Map<String, String> context) {
            super(message);
            this.failureReason = reason;
            this.logContext = Map.copyOf(context);
        }

        public FailureReason reason() {
            return failureReason;
        }

        public Map<String, String> context() {
            return logContext;
        }
    }
}
