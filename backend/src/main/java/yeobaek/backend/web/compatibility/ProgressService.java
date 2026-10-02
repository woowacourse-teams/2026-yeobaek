package yeobaek.backend.web.compatibility;

import static yeobaek.backend.support.LogField.CLUB_ID;
import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.RESULT;
import static yeobaek.backend.support.LogField.SUCCESS;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.book.service.BookCoverUrlResolver;
import yeobaek.backend.club.domain.Club;
import yeobaek.backend.club.dto.ClubBookResponse;
import yeobaek.backend.club.dto.LastReadingResponse;
import yeobaek.backend.club.dto.ProgressResponse;
import yeobaek.backend.club.repository.ClubRepository;
import yeobaek.backend.application.reading.ReadingProgressWorkflow;
import yeobaek.backend.collaboration.api.SpaceContentBindingApi;
import yeobaek.backend.content.api.ContentBodyApi;
import yeobaek.backend.content.api.ContentUnavailableFailure;
import yeobaek.backend.foundation.identity.ContentId;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.foundation.identity.SpaceId;
import yeobaek.backend.reading.domain.ReadingProgress;
import yeobaek.backend.readmodel.reading.RecentReadingReadModel;
import yeobaek.backend.book.domain.BookStatus;
import yeobaek.backend.support.BadRequestException;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.ForbiddenException;
import yeobaek.backend.support.NotFoundException;
import yeobaek.backend.support.InvalidRequestException;
import yeobaek.backend.space.api.SpaceAccessApi;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProgressService {

    private final ClubRepository clubRepository;
    private final SpaceAccessApi spaceAccessApi;
    private final SpaceContentBindingApi bindingApi;
    private final ContentBodyApi contentBodyApi;
    private final BookCoverUrlResolver bookCoverUrlResolver;
    private final ReadingProgressWorkflow readingProgressWorkflow;
    private final RecentReadingReadModel recentReadings;

    @Transactional
    public ProgressResponse updateProgress(Long memberId, Long clubId, Long passageId) {
        log.atInfo().addKeyValue(OPERATION, "progress.updateProgress")
                .addKeyValue(CLUB_ID, clubId)
                .addKeyValue("passageId", passageId).log("진도를 갱신합니다.");
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.CLUB_NOT_FOUND,
                        "진도를 갱신할 모임이 존재하지 않습니다: clubId=" + clubId,
                        Map.of(CLUB_ID, clubId.toString())));
        if (!spaceAccessApi.canAccess(new MemberId(memberId), new SpaceId(club.getSpaceId()))) {
            throw new ForbiddenException(
                    ErrorCode.NOT_CLUB_MEMBER,
                    "모임에 참여 중인 회원만 진도를 갱신할 수 있습니다: clubId=" + clubId,
                    Map.of(CLUB_ID, clubId.toString()));
        }
        SpaceId spaceId = new SpaceId(club.getSpaceId());
        ContentId contentId = requireBoundContent(spaceId, clubId);
        ContentBodyApi.Passage passage = contentBodyApi.findPassage(contentId, passageId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.PASSAGE_NOT_FOUND,
                        "진도를 갱신할 본문이 존재하지 않습니다: passageId=" + passageId,
                        Map.of(CLUB_ID, clubId.toString(),
                                "passageId", passageId.toString())));
        if (!passage.contentId().equals(contentId)) {
            throw new InvalidRequestException(
                    "모임의 도서에 속하지 않는 본문입니다.",
                    Map.of(CLUB_ID, clubId.toString(),
                            "passageId", passageId.toString()));
        }
        ReadingProgress progress;
        try {
            progress = readingProgressWorkflow.update(new MemberId(memberId),
                    spaceId, contentId, passage.locationId(), LocalDateTime.now());
        } catch (ContentUnavailableFailure failure) {
            throw new BadRequestException(ErrorCode.BOOK_NOT_AVAILABLE, failure.getMessage(), failure);
        }
        var response = new ProgressResponse(
                passage.sequence(),
                yeobaek.backend.club.domain.vo.ProgressRate.calculate(
                        passage.sequence(), contentBodyApi.passageCount(contentId)).roundedPercentage(),
                progress.lastReadAt());
        log.atInfo().addKeyValue(OPERATION, "progress.updateProgress").addKeyValue(RESULT, SUCCESS)
                .addKeyValue(CLUB_ID, clubId)
                .addKeyValue("progressRate", response.progressRate()).log("진도를 갱신했습니다.");
        return response;
    }

    private ContentId requireBoundContent(SpaceId spaceId, Long clubId) {
        return bindingApi.findContents(spaceId).stream().findFirst()
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.BOOK_NOT_FOUND,
                        "모임에서 읽는 도서가 존재하지 않습니다: clubId=" + clubId,
                        Map.of(CLUB_ID, clubId.toString())));
    }

    @Transactional(readOnly = true)
    public Optional<LastReadingResponse> findLastReading(Long memberId) {
        log.atInfo().addKeyValue(OPERATION, "progress.findLastReading").log("최근 독서를 조회합니다.");
        Optional<LastReadingResponse> response = recentReadings.findCandidates(new MemberId(memberId)).stream()
                .filter(reading -> reading.space() instanceof yeobaek.backend.space.domain.Club)
                .max(Comparator.comparing(RecentReadingReadModel.RecentReadingSnapshot::lastReadAt))
                .map(this::toLastReadingResponse);
        if (response.isEmpty()) {
            log.atInfo().addKeyValue(OPERATION, "progress.findLastReading").addKeyValue(RESULT, SUCCESS)
                    .addKeyValue("found", false).log("최근 독서를 조회했습니다.");
            return Optional.empty();
        }
        LastReadingResponse found = response.orElseThrow();
        log.atInfo().addKeyValue(OPERATION, "progress.findLastReading").addKeyValue(RESULT, SUCCESS)
                .addKeyValue("found", true).addKeyValue(CLUB_ID, found.clubId())
                .log("최근 독서를 조회했습니다.");
        return response;
    }

    private LastReadingResponse toLastReadingResponse(RecentReadingReadModel.RecentReadingSnapshot reading) {
        var club = (yeobaek.backend.space.domain.Club) reading.space();
        var book = reading.book();
        return new LastReadingResponse(club.clubId(), club.name(),
                new ClubBookResponse(book.bookId(), book.title(), book.authors(),
                        bookCoverUrlResolver.resolve(book.coverImageKey()), book.passageCount(),
                        BookStatus.valueOf(book.status())),
                reading.passageSequence(),
                yeobaek.backend.club.domain.vo.ProgressRate.calculate(
                        reading.passageSequence(), book.passageCount()).roundedPercentage(),
                reading.lastReadAt());
    }
}
