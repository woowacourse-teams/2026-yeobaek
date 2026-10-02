package yeobaek.backend.web.compatibility;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import yeobaek.backend.application.reading.RecentReadingWorkflow;
import yeobaek.backend.application.reading.RecentReadingWorkflow.BookResult;
import yeobaek.backend.book.domain.BookStatus;
import yeobaek.backend.book.service.BookCoverUrlResolver;
import yeobaek.backend.club.dto.ClubBookResponse;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.publicroom.dto.ClubReadingSpaceResponse;
import yeobaek.backend.publicroom.dto.PublicRoomReadingSpaceResponse;
import yeobaek.backend.publicroom.dto.ReadingSpaceResponse;
import yeobaek.backend.publicroom.dto.RecentReadingResponse;
import yeobaek.backend.space.domain.Club;
import yeobaek.backend.space.domain.PublicRoom;
import yeobaek.backend.space.domain.Space;

@Service
@RequiredArgsConstructor
public class RecentReadingService {

    private final RecentReadingWorkflow workflow;
    private final BookCoverUrlResolver coverUrlResolver;

    public Optional<RecentReadingResponse> findRecent(Long memberId) {
        return workflow.findRecent(new MemberId(memberId)).map(result -> new RecentReadingResponse(
                toSpace(result.space(), result.spaceName()), toBook(result.book()), result.passageSequence(),
                result.progressRate(), result.lastReadAt()));
    }

    private ReadingSpaceResponse toSpace(Space space, String spaceName) {
        if (space instanceof Club club) {
            return new ClubReadingSpaceResponse(club.clubId(), spaceName);
        }
        if (space instanceof PublicRoom room) {
            return new PublicRoomReadingSpaceResponse(room.publicRoomId());
        }
        throw new IllegalArgumentException("지원하지 않는 v1 독서 공간입니다: " + space.kind());
    }

    private ClubBookResponse toBook(BookResult book) {
        return new ClubBookResponse(book.bookId(), book.title(), book.authors(),
                coverUrlResolver.resolve(book.coverImageKey()), book.passageCount(), BookStatus.valueOf(book.status()));
    }
}
