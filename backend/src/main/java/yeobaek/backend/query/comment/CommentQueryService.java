package yeobaek.backend.query.comment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.appreciation.api.comment.CommentApi;
import yeobaek.backend.appreciation.api.comment.CommentResponse;
import yeobaek.backend.appreciation.api.comment.CommentViewApi;
import yeobaek.backend.collaboration.api.context.AppreciationContextApi;
import yeobaek.backend.collaboration.api.context.AppreciationContextApi.LocatedAppreciation;
import yeobaek.backend.content.api.location.ContentLegacyLocationQueryApi;
import yeobaek.backend.member.api.block.MemberBlockApi;
import yeobaek.backend.shared.identity.AppreciationId;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.ContentLocationId;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentQueryService {

    private final ContentLegacyLocationQueryApi locations;
    private final AppreciationContextApi contexts;
    private final CommentApi comments;
    private final CommentViewApi views;
    private final MemberBlockApi blocks;

    public List<CommentResponse> findVisible(
            MemberId requesterId,
            SpaceId spaceId,
            ContentLocationId locationId
    ) {
        return visible(requesterId, contexts.findInSpace(spaceId).stream()
                .filter(context -> context.locationId().equals(locationId)).toList());
    }

    public long countNewVisible(MemberId requesterId, SpaceId spaceId, ContentId contentId,
                                int currentPassageSequence) {
        List<LocatedAppreciation> located = findInContent(spaceId, contentId);
        Set<ContentLocationId> eligibleLocations = locations.findSentenceInfo(
                        located.stream().map(LocatedAppreciation::locationId).toList()).stream()
                .filter(sentence -> sentence.passageSequence() <= currentPassageSequence)
                .map(ContentLegacyLocationQueryApi.SentenceInfo::locationId).collect(Collectors.toSet());
        List<CommentResponse> visible = visible(requesterId, located.stream()
                .filter(context -> eligibleLocations.contains(context.locationId())).toList());
        Set<AppreciationId> viewed = views.findViewedIds(requesterId,
                visible.stream().map(CommentResponse::id).toList());
        return visible.stream().filter(comment -> !viewed.contains(comment.id())).count();
    }

    public List<DiscoverySnapshot> findDiscovery(MemberId requesterId, SpaceId spaceId, ContentId contentId) {
        List<LocatedAppreciation> located = findInContent(spaceId, contentId);
        Map<AppreciationId, ContentLocationId> locationByComment = located.stream()
                .collect(Collectors.toMap(LocatedAppreciation::appreciationId, LocatedAppreciation::locationId));
        List<CommentResponse> visible = visible(requesterId, located);
        Map<ContentLocationId, List<CommentResponse>> commentsByLocation = visible.stream()
                .collect(Collectors.groupingBy(comment -> locationByComment.get(comment.id())));
        Set<AppreciationId> viewed = views.findViewedIds(requesterId,
                visible.stream().map(CommentResponse::id).toList());
        return locations.findSentenceInfo(commentsByLocation.keySet()).stream().map(sentence -> {
            List<CommentResponse> atLocation = commentsByLocation.get(sentence.locationId());
            long unread = atLocation.stream().filter(comment -> !viewed.contains(comment.id())).count();
            LocalDateTime latest = atLocation.stream().map(CommentResponse::createdAt)
                    .max(LocalDateTime::compareTo).orElseThrow();
            ContentLocationId passageLocationId = locations.findPassage(sentence.passageId()).orElseThrow().locationId();
            return new DiscoverySnapshot(sentence.locationId(), passageLocationId, sentence.sentenceId(),
                    sentence.content(),
                    sentence.passageSequence(), sentence.sentenceSequence(), atLocation.size(), unread, latest);
        }).toList();
    }

    private List<CommentResponse> visible(MemberId requesterId, List<LocatedAppreciation> located) {
        List<CommentResponse> candidates = comments.findByIds(
                located.stream().map(LocatedAppreciation::appreciationId).toList());
        Set<MemberId> excluded = blocks.findBlockedMemberIds(requesterId,
                candidates.stream().map(CommentResponse::authorId).collect(Collectors.toSet()));
        return candidates.stream().filter(comment -> !excluded.contains(comment.authorId())).toList();
    }

    private List<LocatedAppreciation> findInContent(SpaceId spaceId, ContentId contentId) {
        return contexts.findInSpace(spaceId).stream()
                .filter(context -> context.contentId().equals(contentId))
                .toList();
    }

    public record DiscoverySnapshot(ContentLocationId locationId, ContentLocationId passageLocationId,
                                    long legacySentenceId, String content, int passageSequence,
                                    int sentenceSequence, long commentCount, long unreadCommentCount,
                                    LocalDateTime latestCommentCreatedAt) {
    }
}
