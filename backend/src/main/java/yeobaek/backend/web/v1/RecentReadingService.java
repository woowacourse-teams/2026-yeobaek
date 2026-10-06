package yeobaek.backend.web.v1;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import yeobaek.backend.application.content.ContentCardResult;
import yeobaek.backend.application.reading.ReadingActivityQueryService;
import yeobaek.backend.application.reading.ReadingActivityResult;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.content.api.idmapping.ContentIdMappingApi;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.space.api.SpaceKind;
import yeobaek.backend.space.api.club.ClubApi;
import yeobaek.backend.space.api.publicroom.PublicRoomApi;
import yeobaek.backend.web.common.dto.BookResponse;
import yeobaek.backend.web.common.dto.BookResponse.Status;
import yeobaek.backend.web.publicroom.dto.ClubReadingSpaceResponse;
import yeobaek.backend.web.publicroom.dto.PublicRoomReadingSpaceResponse;
import yeobaek.backend.web.publicroom.dto.ReadingSpaceResponse;
import yeobaek.backend.web.publicroom.dto.RecentReadingResponse;

@Service
@RequiredArgsConstructor
public class RecentReadingService {

    private final ReadingActivityQueryService activities;
    private final ClubApi clubs;
    private final PublicRoomApi rooms;
    private final ContentIdMappingApi idMappings;

    public Optional<RecentReadingResponse> findRecent(Long memberId) {
        return activities.findRecent(new MemberId(memberId)).map(result -> new RecentReadingResponse(
                space(result.space()), book(result.content()), result.lastReadPassageSequence(),
                result.progressRate(), result.lastReadAt()));
    }

    private ReadingSpaceResponse space(ReadingActivityResult.ReadingSpace space) {
        if (SpaceKind.CLUB.equals(space.kind())) {
            var club = clubs.findBySpaceId(space.spaceId()).orElseThrow();
            return new ClubReadingSpaceResponse(club.clubId(),
                    ((ReadingActivityResult.ReadingSpace.Club) space.data()).name());
        }
        var room = rooms.findBySpaceId(space.spaceId()).orElseThrow();
        return new PublicRoomReadingSpaceResponse(room.publicRoomId());
    }

    private BookResponse book(ContentCardResult content) {
        Long bookId = idMappings.toImplementationIds(ContentKind.BOOK, List.of(content.contentId()))
                .get(content.contentId());
        return new BookResponse(bookId, content.title(), content.creators(), content.coverImageUrl(),
                content.unitCount(), content.available() ? Status.ACTIVE : Status.DELETED);
    }
}
