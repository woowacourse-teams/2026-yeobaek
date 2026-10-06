package yeobaek.backend.application.appreciation;

import static yeobaek.backend.support.LogField.ACTOR_ID;
import static yeobaek.backend.support.LogField.CONTENT_ID;
import static yeobaek.backend.support.LogField.LOCATION_ID;
import static yeobaek.backend.support.LogField.REASON;
import static yeobaek.backend.support.LogField.SPACE_ID;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.appreciation.api.comment.CommentResponse;
import yeobaek.backend.appreciation.api.comment.CommentViewApi;
import yeobaek.backend.collaboration.api.binding.SpaceContentBindingApi;
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
import yeobaek.backend.readmodel.comment.CommentReadModel;
import yeobaek.backend.readmodel.comment.CommentReadModel.DiscoverySnapshot;
import yeobaek.backend.space.api.access.SpaceAccessApi;

@Service
@RequiredArgsConstructor
public class CommentQueryWorkflow {

    private final CommentReadModel comments;
    private final CommentViewApi views;
    private final MemberQuery members;
    private final SpaceAccessApi spaces;
    private final SpaceContentBindingApi bindings;
    private final ContentApi contents;
    private final ContentLocationQueryApi locations;

    @Transactional
    public List<CommentResult> findComments(MemberId requesterId, SpaceId spaceId, ContentId contentId,
                                            ContentLocationId locationId) {
        requireTarget(requesterId, spaceId, contentId, locationId, LocationKind.SENTENCE);
        List<CommentResponse> visible = comments.findVisible(requesterId, spaceId, locationId);
        Map<MemberId, MemberProfile> profiles = profiles(visible);
        List<CommentResult> results = visible.stream()
                .map(comment -> new CommentResult(comment, profiles.get(comment.authorId())))
                .toList();
        views.markViewed(requesterId, visible.stream().map(CommentResponse::id).toList());
        return results;
    }

    @Transactional(readOnly = true)
    public long countNewComments(MemberId requesterId, SpaceId spaceId, ContentId contentId,
                                 ContentLocationId currentLocationId) {
        var passage = requireTarget(requesterId, spaceId, contentId, currentLocationId, LocationKind.PASSAGE);
        return comments.countNewVisible(requesterId, spaceId, contentId, passage.passageSequence());
    }

    @Transactional(readOnly = true)
    public List<DiscoveryResult> findDiscovery(MemberId requesterId, SpaceId spaceId, ContentId contentId,
                                               ContentLocationId currentLocationId) {
        var passage = requireTarget(requesterId, spaceId, contentId, currentLocationId, LocationKind.PASSAGE);
        return comments.findDiscovery(requesterId, spaceId, contentId).stream()
                .sorted(discoveryComparator(passage.passageSequence()))
                .map(snapshot -> toDiscovery(snapshot, passage.passageSequence()))
                .toList();
    }

    private ContentLocationQueryApi.Location requireTarget(MemberId requesterId, SpaceId spaceId,
                                                            ContentId contentId, ContentLocationId locationId,
                                                            LocationKind expectedKind) {
        members.getProfile(requesterId);
        if (!spaces.canAccess(requesterId, spaceId)) {
            throw new CommentQueryException(ErrorCode.SPACE_ACCESS_DENIED,
                    "공간에 접근할 수 있는 회원만 댓글 정보를 조회할 수 있습니다: spaceId=" + spaceId.value(),
                    Map.of(ACTOR_ID, Long.toString(requesterId.value()),
                            SPACE_ID, Long.toString(spaceId.value()),
                            CONTENT_ID, Long.toString(contentId.value()),
                            LOCATION_ID, Long.toString(locationId.value()),
                            REASON, "SPACE_ACCESS_DENIED"));
        }
        if (!bindings.isBound(spaceId, contentId)) {
            throw new CommentQueryException(ErrorCode.INVALID_REQUEST,
                    "댓글 정보를 조회할 공간에 연결되지 않은 컨텐츠입니다: spaceId=" + spaceId.value()
                            + ", contentId=" + contentId.value(),
                    Map.of(ACTOR_ID, Long.toString(requesterId.value()),
                            SPACE_ID, Long.toString(spaceId.value()),
                            CONTENT_ID, Long.toString(contentId.value()),
                            LOCATION_ID, Long.toString(locationId.value()),
                            REASON, "INVALID_REQUEST"));
        }
        var location = locations.get(locationId);
        if (!location.contentId().equals(contentId) || !expectedKind.equals(location.kind())) {
            throw new CommentQueryException(ErrorCode.INVALID_REQUEST,
                    "댓글 정보를 조회할 컨텐츠 위치가 올바르지 않습니다: locationId=" + locationId.value(),
                    Map.of(ACTOR_ID, Long.toString(requesterId.value()),
                            SPACE_ID, Long.toString(spaceId.value()),
                            CONTENT_ID, Long.toString(contentId.value()),
                            LOCATION_ID, Long.toString(locationId.value()),
                            "expectedLocationKind", expectedKind.value(),
                            "actualLocationKind", location.kind().value(),
                            REASON, "INVALID_REQUEST"));
        }
        if (!contents.getContent(contentId).available()) {
            throw new CommentQueryException(ErrorCode.CONTENT_UNAVAILABLE,
                    "댓글 정보를 조회할 컨텐츠를 더 이상 이용할 수 없습니다: contentId=" + contentId.value(),
                    Map.of("actorId", Long.toString(requesterId.value()),
                            "spaceId", Long.toString(spaceId.value()),
                            "contentId", Long.toString(contentId.value()),
                            "locationId", Long.toString(locationId.value()),
                            "reason", "CONTENT_UNAVAILABLE"));
        }
        return location;
    }

    private Map<MemberId, MemberProfile> profiles(List<CommentResponse> visible) {
        if (visible.isEmpty()) {
            return Map.of();
        }
        Set<MemberId> authorIds = visible.stream().map(CommentResponse::authorId).collect(Collectors.toSet());
        return members.findProfiles(authorIds);
    }

    private DiscoveryResult toDiscovery(DiscoverySnapshot snapshot, int currentPassageSequence) {
        boolean future = snapshot.passageSequence() > currentPassageSequence;
        return new DiscoveryResult(snapshot.locationId(), snapshot.content(), snapshot.passageLocationId(),
                snapshot.passageSequence(), snapshot.sentenceSequence(), future, snapshot.commentCount(),
                snapshot.unreadCommentCount(), future && snapshot.unreadCommentCount() > 0,
                snapshot.latestCommentCreatedAt());
    }

    private Comparator<DiscoverySnapshot> discoveryComparator(int currentPassageSequence) {
        return Comparator.comparingInt((DiscoverySnapshot snapshot) -> group(snapshot, currentPassageSequence))
                .thenComparingInt(DiscoverySnapshot::passageSequence)
                .thenComparingInt(DiscoverySnapshot::sentenceSequence)
                .thenComparing(DiscoverySnapshot::legacySentenceId, Comparator.reverseOrder());
    }

    private int group(DiscoverySnapshot snapshot, int currentPassageSequence) {
        if (snapshot.unreadCommentCount() == 0) {
            return 2;
        }
        return snapshot.passageSequence() > currentPassageSequence ? 1 : 0;
    }

    public record CommentResult(CommentResponse comment, MemberProfile author) {
    }

    public record DiscoveryResult(ContentLocationId locationId, String content,
                                  ContentLocationId passageLocationId, int passageSequence, int sentenceSequence,
                                  boolean future, long commentCount, long unreadCommentCount,
                                  boolean revealRequired, LocalDateTime latestCommentCreatedAt) {
    }

}
