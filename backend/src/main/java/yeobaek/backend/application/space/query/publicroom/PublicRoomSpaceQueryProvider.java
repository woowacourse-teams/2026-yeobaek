package yeobaek.backend.application.space.query.publicroom;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.application.space.query.PublicSpaceListProvider;
import yeobaek.backend.application.space.query.SpaceQueryPolicyException;
import yeobaek.backend.application.space.query.SpaceQueryProvider;
import yeobaek.backend.application.space.query.SpaceQueryResult;
import yeobaek.backend.application.space.query.SpaceQueryResult.PublicRoomDetail;
import yeobaek.backend.application.space.query.SpaceQueryResult.PublicRoomSummary;
import yeobaek.backend.application.space.query.SpaceQueryResult.PublicRoomVisitedSummary;
import yeobaek.backend.application.space.query.SpaceQueryResultFactory;
import yeobaek.backend.application.content.ContentCardResult;
import yeobaek.backend.collaboration.api.binding.SpaceContentBindingApi;
import yeobaek.backend.content.api.ContentApi;
import yeobaek.backend.content.api.ContentNotFoundException;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.member.api.MemberQuery;
import yeobaek.backend.reading.api.ReadingProgressApi;
import yeobaek.backend.space.api.SpaceKind;
import yeobaek.backend.space.api.SpaceNotFoundException;
import yeobaek.backend.space.api.publicroom.PublicRoomApi;
import yeobaek.backend.space.api.publicroom.PublicRoomResponse;
import yeobaek.backend.space.api.publicroom.PublicRoomVisitApi;
import yeobaek.backend.space.api.publicroom.PublicRoomVisitResponse;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicRoomSpaceQueryProvider implements SpaceQueryProvider, PublicSpaceListProvider {

    private final PublicRoomApi rooms;
    private final PublicRoomVisitApi visits;
    private final SpaceContentBindingApi bindings;
    private final ContentApi contents;
    private final ReadingProgressApi readings;
    private final MemberQuery members;
    private final SpaceQueryResultFactory results;

    @Override
    public SpaceKind supportedKind() {
        return SpaceKind.PUBLIC_ROOM;
    }

    @Override
    public List<SpaceQueryResult> findMine(MemberId actorId) {
        members.getProfile(actorId);
        return visits.findVisits(actorId).stream()
                .filter(visit -> visit.lastVisitedAt() != null)
                .flatMap(visit -> rooms.findBySpaceId(visit.spaceId()).stream()
                        .flatMap(room -> activeResult(actorId, room, visit.lastVisitedAt()).stream()))
                .toList();
    }

    @Override
    public SpaceQueryResult findDetail(MemberId actorId, SpaceId spaceId) {
        members.getProfile(actorId);
        rooms.findBySpaceId(spaceId).orElseThrow(() -> new SpaceNotFoundException(spaceId,
                "조회할 공개방 공간이 존재하지 않습니다: spaceId=" + spaceId.value()));
        ContentId contentId = requireContent(spaceId);
        var content = results.content(contentId);
        var progress = readings.find(actorId, spaceId, contentId).orElse(null);
        LocalDateTime visitedAt = visits.findLastVisit(actorId, spaceId)
                .map(PublicRoomVisitResponse::lastVisitedAt).orElse(null);
        return new SpaceQueryResult(spaceId, SpaceKind.PUBLIC_ROOM,
                new PublicRoomDetail(content, results.progress(progress, content.unitCount()), visitedAt));
    }

    @Override
    public List<SpaceQueryResult> findPublic(MemberId actorId) {
        members.getProfile(actorId);
        List<PublicRoomResponse> found = rooms.findAll();
        List<SpaceId> spaceIds = found.stream().map(PublicRoomResponse::id).toList();
        Map<SpaceId, Long> visitorCounts = visits.countVisitors(spaceIds);
        Map<SpaceId, yeobaek.backend.reading.api.ReadingProgress> progressBySpace = readings
                .findBySpaces(actorId, spaceIds).stream()
                .collect(java.util.stream.Collectors.toMap(progress -> progress.spaceId(), progress -> progress));
        return found.stream()
                .map(room -> activeContent(room.id()).map(content -> new ActiveRoom(room, content)))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparingLong((ActiveRoom active) ->
                        visitorCounts.getOrDefault(active.room().id(), 0L)).reversed())
                .map(active -> summary(active.room(), active.content(), progressBySpace.get(active.room().id()), null))
                .toList();
    }

    private Optional<SpaceQueryResult> activeResult(MemberId actorId, PublicRoomResponse room,
                                                    LocalDateTime visitedAt) {
        return activeContent(room.id()).map(content -> summary(room, content,
                readings.find(actorId, room.id(), content.contentId()).orElse(null), visitedAt));
    }

    private SpaceQueryResult summary(PublicRoomResponse room, ContentCardResult content,
                                     yeobaek.backend.reading.api.ReadingProgress progress, LocalDateTime visitedAt) {
        var mappedProgress = results.progress(progress, content.unitCount());
        if (visitedAt != null) {
            return new SpaceQueryResult(room.id(), SpaceKind.PUBLIC_ROOM,
                    new PublicRoomVisitedSummary(content, mappedProgress, visitedAt));
        }
        return new SpaceQueryResult(room.id(), SpaceKind.PUBLIC_ROOM,
                new PublicRoomSummary(content, mappedProgress));
    }

    private Optional<ContentCardResult> activeContent(SpaceId spaceId) {
        Optional<ContentId> contentId = bindings.findContents(spaceId).stream().findFirst();
        if (contentId.isEmpty()) {
            return Optional.empty();
        }
        try {
            if (!contents.getContent(contentId.get()).available()) {
                return Optional.empty();
            }
            return Optional.of(results.content(contentId.get()));
        } catch (ContentNotFoundException failure) {
            return Optional.empty();
        }
    }

    private ContentId requireContent(SpaceId spaceId) {
        return bindings.findContents(spaceId).stream().findFirst()
                .orElseThrow(() -> new SpaceQueryPolicyException(ErrorCode.CONTENT_NOT_BOUND,
                        "공개방에 연결된 컨텐츠가 없습니다: spaceId=" + spaceId.value(),
                        Map.of("spaceId", Long.toString(spaceId.value()), "reason", "CONTENT_NOT_BOUND")));
    }

    private record ActiveRoom(PublicRoomResponse room, ContentCardResult content) {
    }
}
