package yeobaek.backend.web.v1;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import yeobaek.backend.application.content.ContentCardResult;
import yeobaek.backend.application.content.ContentReadingQueryService;
import yeobaek.backend.application.publicroom.PublicRoomVisitWorkflow;
import yeobaek.backend.application.reading.ReadingProgressCommandService;
import yeobaek.backend.application.space.query.SpaceQueryResult;
import yeobaek.backend.application.space.query.SpaceQueryService;
import yeobaek.backend.collaboration.api.binding.SpaceContentBindingApi;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.content.api.legacyreference.ContentLegacyReferenceApi;
import yeobaek.backend.content.api.location.ContentLegacyLocationQueryApi;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.space.api.SpaceKind;
import yeobaek.backend.space.api.publicroom.PublicRoomApi;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.shared.exception.NotFoundException;
import yeobaek.backend.web.book.dto.PassageResponse;
import yeobaek.backend.web.book.dto.PassagesResponse;
import yeobaek.backend.web.book.dto.SentenceResponse;
import yeobaek.backend.web.common.dto.BookResponse;
import yeobaek.backend.web.common.dto.BookResponse.Status;
import yeobaek.backend.web.publicroom.dto.PublicRoomDetailResponse;
import yeobaek.backend.web.publicroom.dto.PublicRoomProgressResponse;
import yeobaek.backend.web.publicroom.dto.PublicRoomResponse;
import yeobaek.backend.web.publicroom.dto.PublicRoomSort;
import yeobaek.backend.web.publicroom.dto.PublicRoomsResponse;
import yeobaek.backend.web.publicroom.dto.VisitedPublicRoomResponse;
import yeobaek.backend.web.publicroom.dto.VisitedPublicRoomsResponse;

@Service("publicRoomCompatibilityService")
@RequiredArgsConstructor
public class PublicRoomService {

    private final PublicRoomApi rooms;
    private final SpaceContentBindingApi bindings;
    private final ContentLegacyReferenceApi contentReferences;
    private final ContentLegacyLocationQueryApi locations;
    private final SpaceQueryService spaceQueries;
    private final ContentReadingQueryService contentQueries;
    private final ReadingProgressCommandService progressCommands;
    private final PublicRoomVisitWorkflow visits;

    public PublicRoomsResponse findAll(Long memberId, PublicRoomSort sort) {
        return new PublicRoomsResponse(spaceQueries.findPublic(new MemberId(memberId), SpaceKind.PUBLIC_ROOM).stream()
                .map(this::publicRoom).toList());
    }

    public VisitedPublicRoomsResponse findVisited(Long memberId) {
        return new VisitedPublicRoomsResponse(spaceQueries.findMine(new MemberId(memberId), SpaceKind.PUBLIC_ROOM)
                .stream().map(result -> {
                    var data = (SpaceQueryResult.PublicRoomVisitedSummary) result.data();
                    return new VisitedPublicRoomResponse(legacyRoomId(result), book(data.content()),
                            progress(data.myProgress()), data.lastVisitedAt());
                }).toList());
    }

    public PublicRoomDetailResponse findDetail(Long memberId, Long publicRoomId) {
        var room = requireRoom(publicRoomId);
        var result = spaceQueries.findDetail(new MemberId(memberId), room.id());
        var data = (SpaceQueryResult.PublicRoomDetail) result.data();
        return new PublicRoomDetailResponse(publicRoomId, book(data.content()), progress(data.myProgress()),
                data.lastVisitedAt());
    }

    public void visit(Long memberId, Long publicRoomId) {
        visits.visit(new MemberId(memberId), requireRoom(publicRoomId).id());
    }

    public PassagesResponse findPassages(Long memberId, Long publicRoomId, int from, int to) {
        var room = requireRoom(publicRoomId);
        ContentId contentId = requireContent(room.id());
        var body = contentQueries.findBody(new MemberId(memberId), room.id(), contentId, from, to);
        var sentenceInfo = locations.findSentenceInfo(body.passages().stream()
                .flatMap(passage -> passage.sentences().stream())
                .map(ContentReadingQueryService.SentenceResult::locationId).toList()).stream()
                .collect(java.util.stream.Collectors.toMap(info -> info.locationId(), info -> info));
        return new PassagesResponse(body.passages().stream().map(passage -> new PassageResponse(
                locations.findByLocation(passage.locationId()).orElseThrow().passageId(), passage.sequence(),
                passage.sectionId(), passage.sentences().stream().map(sentence -> new SentenceResponse(
                        sentenceInfo.get(sentence.locationId()).sentenceId(), sentence.sequence(), sentence.content(),
                        sentence.commentCount())).toList())).toList());
    }

    public PublicRoomProgressResponse updateProgress(Long memberId, Long publicRoomId, Long passageId) {
        var room = requireRoom(publicRoomId);
        ContentId contentId = requireContent(room.id());
        var passage = locations.findPassage(passageId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PASSAGE_NOT_FOUND,
                        "진도를 갱신할 본문이 존재하지 않습니다: passageId=" + passageId,
                        Map.of("actorId", memberId.toString(), "publicRoomId", publicRoomId.toString(),
                                "passageId", passageId.toString())));
        var changed = progressCommands.update(new MemberId(memberId), room.id(), contentId, passage.locationId());
        return new PublicRoomProgressResponse(changed.lastReadPassageSequence(), changed.progressRate(),
                changed.lastReadAt());
    }

    private PublicRoomResponse publicRoom(SpaceQueryResult result) {
        var data = (SpaceQueryResult.PublicRoomSummary) result.data();
        return new PublicRoomResponse(legacyRoomId(result), book(data.content()), progress(data.myProgress()));
    }

    private Long legacyRoomId(SpaceQueryResult result) {
        return rooms.findBySpaceId(result.spaceId()).orElseThrow().publicRoomId();
    }

    private BookResponse book(ContentCardResult content) {
        Long bookId = contentReferences.legacyIds(ContentKind.BOOK, List.of(content.contentId()))
                .get(content.contentId());
        return new BookResponse(bookId, content.title(), content.creators(), content.coverImageUrl(),
                content.unitCount(), content.available() ? Status.ACTIVE : Status.DELETED);
    }

    private PublicRoomProgressResponse progress(SpaceQueryResult.Progress progress) {
        if (progress == null) {
            return null;
        }
        return new PublicRoomProgressResponse(progress.lastReadPassageSequence(), progress.progressRate(),
                progress.lastReadAt());
    }

    private yeobaek.backend.space.api.publicroom.PublicRoomResponse requireRoom(Long publicRoomId) {
        return rooms.findById(publicRoomId).orElseThrow(() -> new NotFoundException(
                ErrorCode.PUBLIC_ROOM_NOT_FOUND, "공개방이 존재하지 않습니다: publicRoomId=" + publicRoomId,
                Map.of("publicRoomId", publicRoomId.toString())));
    }

    private ContentId requireContent(yeobaek.backend.shared.identity.SpaceId spaceId) {
        return bindings.findContents(spaceId).stream().findFirst()
                .orElseThrow(() -> new NotFoundException(ErrorCode.BOOK_NOT_FOUND,
                        "공개방에 연결된 도서가 존재하지 않습니다.",
                        Map.of("spaceId", Long.toString(spaceId.value()))));
    }
}
