package yeobaek.backend.web.v1;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import yeobaek.backend.application.content.ContentCardResult;
import yeobaek.backend.application.reading.ReadingActivityQueryService;
import yeobaek.backend.application.reading.ReadingActivityResult;
import yeobaek.backend.application.reading.ReadingProgressCommandService;
import yeobaek.backend.collaboration.api.binding.SpaceContentBindingApi;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.content.api.idmapping.ContentIdMappingApi;
import yeobaek.backend.content.api.location.ContentLegacyLocationQueryApi;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.space.api.club.ClubApi;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.shared.exception.NotFoundException;
import yeobaek.backend.web.club.dto.LastReadingResponse;
import yeobaek.backend.web.club.dto.ProgressResponse;
import yeobaek.backend.web.common.dto.BookResponse;
import yeobaek.backend.web.common.dto.BookResponse.Status;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final ClubApi clubs;
    private final SpaceContentBindingApi bindings;
    private final ContentLegacyLocationQueryApi locations;
    private final ContentIdMappingApi idMappings;
    private final ReadingProgressCommandService commands;
    private final ReadingActivityQueryService activities;

    public ProgressResponse updateProgress(Long memberId, Long clubId, Long passageId) {
        var club = clubs.findById(clubId).orElseThrow(() -> new NotFoundException(ErrorCode.CLUB_NOT_FOUND,
                "진도를 갱신할 모임이 존재하지 않습니다: clubId=" + clubId,
                Map.of("clubId", clubId.toString(), "actorId", memberId.toString())));
        ContentId contentId = requireContent(club.id());
        var passage = locations.findPassage(passageId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PASSAGE_NOT_FOUND,
                        "진도를 갱신할 본문이 존재하지 않습니다: passageId=" + passageId,
                        Map.of("clubId", clubId.toString(), "passageId", passageId.toString(),
                                "actorId", memberId.toString())));
        var changed = commands.update(new MemberId(memberId), club.id(), contentId, passage.locationId());
        return new ProgressResponse(changed.lastReadPassageSequence(), changed.progressRate(), changed.lastReadAt());
    }

    public Optional<LastReadingResponse> findLastReading(Long memberId) {
        return activities.findLastClub(new MemberId(memberId)).map(this::lastReading);
    }

    private LastReadingResponse lastReading(ReadingActivityResult reading) {
        var club = clubs.findBySpaceId(reading.space().spaceId()).orElseThrow();
        return new LastReadingResponse(club.clubId(), ((ReadingActivityResult.ReadingSpace.Club) reading.space().data()).name(),
                book(reading.content()), reading.lastReadPassageSequence(), reading.progressRate(),
                reading.lastReadAt());
    }

    private BookResponse book(ContentCardResult content) {
        Long bookId = idMappings.toImplementationIds(ContentKind.BOOK, List.of(content.contentId()))
                .get(content.contentId());
        return new BookResponse(bookId, content.title(), content.creators(), content.coverImageUrl(),
                content.unitCount(), content.available() ? Status.ACTIVE : Status.DELETED);
    }

    private ContentId requireContent(yeobaek.backend.shared.identity.SpaceId spaceId) {
        return bindings.findContents(spaceId).stream().findFirst()
                .orElseThrow(() -> new NotFoundException(ErrorCode.BOOK_NOT_FOUND,
                        "모임에서 읽는 도서가 존재하지 않습니다.",
                        Map.of("spaceId", Long.toString(spaceId.value()))));
    }
}
