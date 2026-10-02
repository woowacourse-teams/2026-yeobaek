package yeobaek.backend.web.compatibility;

import static yeobaek.backend.support.LogField.CLUB_ID;
import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.RESULT;
import static yeobaek.backend.support.LogField.REASON;
import static yeobaek.backend.support.LogField.SUCCESS;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.book.domain.vo.PassageRange;
import yeobaek.backend.book.dto.PassageResponse;
import yeobaek.backend.book.dto.PassagesResponse;
import yeobaek.backend.book.dto.SentenceResponse;
import yeobaek.backend.club.domain.Club;
import yeobaek.backend.club.repository.ClubRepository;
import yeobaek.backend.appreciation.api.CommentQueryApi;
import yeobaek.backend.collaboration.api.SpaceContentBindingApi;
import yeobaek.backend.content.api.ContentApi;
import yeobaek.backend.content.api.ContentBodyApi;
import yeobaek.backend.content.api.ContentUnavailableFailure;
import yeobaek.backend.foundation.identity.ContentId;
import yeobaek.backend.foundation.identity.ContentLocationId;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.foundation.identity.SpaceId;
import yeobaek.backend.space.api.SpaceAccessApi;
import yeobaek.backend.member.api.MemberBlockApi;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.BadRequestException;
import yeobaek.backend.support.ForbiddenException;
import yeobaek.backend.support.NotFoundException;
import yeobaek.backend.support.InvalidRequestException;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PassageService {

    private static final int MAX_RANGE_SIZE = 100;

    private final ClubRepository clubRepository;
    private final SpaceContentBindingApi bindingApi;
    private final ContentApi contentApi;
    private final ContentBodyApi contentBodyApi;
    private final CommentQueryApi commentQueryApi;
    private final SpaceAccessApi spaceAccessApi;
    private final MemberBlockApi memberBlockApi;

    public PassagesResponse findPassages(Long memberId, Long clubId, int from, int to) {
        log.atInfo().addKeyValue(OPERATION, "passage.findPassages")
                .addKeyValue(CLUB_ID, clubId)
                .addKeyValue("from", from).addKeyValue("to", to).log("본문 범위를 조회합니다.");
        PassageRange range = new PassageRange(from, to);
        validateRangeSize(range);
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.CLUB_NOT_FOUND,
                        "본문을 조회할 모임이 존재하지 않습니다: clubId=" + clubId,
                        Map.of(CLUB_ID, clubId.toString())));
        SpaceId spaceId = new SpaceId(club.getSpaceId());
        if (!spaceAccessApi.canAccess(new MemberId(memberId), spaceId)) {
            throw new ForbiddenException(
                    ErrorCode.NOT_CLUB_MEMBER,
                    "모임에 참여 중인 회원만 본문을 조회할 수 있습니다: clubId=" + clubId,
                    Map.of(CLUB_ID, clubId.toString()));
        }
        ContentId contentId = requireBoundContent(spaceId, clubId);
        requireAvailable(contentId);
        List<ContentBodyApi.Passage> passages = contentBodyApi.findPassages(contentId, range.from(), range.to());
        Map<Long, ContentLocationId> locationsBySentenceId = passages.stream()
                .flatMap(passage -> passage.sentences().stream())
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        ContentBodyApi.Sentence::sentenceId,
                        ContentBodyApi.Sentence::locationId));
        Map<Long, Long> commentCounts = countComments(memberId, spaceId, locationsBySentenceId);
        var response = new PassagesResponse(passages.stream()
                .map(passage -> new PassageResponse(passage.passageId(), passage.sequence(),
                        passage.chapterId(), passage.sentences().stream()
                        .map(sentence -> new SentenceResponse(sentence.sentenceId(), sentence.sequence(),
                                sentence.content(), commentCounts.getOrDefault(sentence.sentenceId(), 0L)))
                        .toList()))
                .toList());
        log.atInfo().addKeyValue(OPERATION, "passage.findPassages").addKeyValue(RESULT, SUCCESS)
                .addKeyValue(CLUB_ID, clubId)
                .addKeyValue("resultCount", response.passages().size()).log("본문 범위를 조회했습니다.");
        return response;
    }

    private ContentId requireBoundContent(SpaceId spaceId, Long clubId) {
        return bindingApi.findContents(spaceId).stream().findFirst()
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.BOOK_NOT_FOUND,
                        "모임에서 읽는 도서가 존재하지 않습니다: clubId=" + clubId,
                        Map.of(CLUB_ID, clubId.toString())));
    }

    private Map<Long, Long> countComments(Long memberId, SpaceId spaceId,
                                          Map<Long, ContentLocationId> locationsBySentenceId) {
        if (locationsBySentenceId.isEmpty()) {
            return Map.of();
        }
        Map<ContentLocationId, Long> counts = commentQueryApi.countByLocation(
                spaceId, List.copyOf(locationsBySentenceId.values()),
                memberBlockApi.findBlockedProfiles(new MemberId(memberId)).keySet());
        return locationsBySentenceId.entrySet().stream()
                .filter(entry -> counts.containsKey(entry.getValue()))
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        Map.Entry::getKey, entry -> counts.get(entry.getValue())));
    }

    private void requireAvailable(ContentId contentId) {
        try {
            contentApi.requireAvailable(contentId);
        } catch (ContentUnavailableFailure failure) {
            throw new BadRequestException(
                    ErrorCode.BOOK_NOT_AVAILABLE,
                    "삭제된 도서의 본문은 조회할 수 없습니다: contentId=" + contentId.value(),
                    Map.of("bookId", contentId.value().toString()), failure);
        }
    }

    private void validateRangeSize(PassageRange range) {
        if (range.size() > MAX_RANGE_SIZE) {
            throw new InvalidRequestException(
                    "본문은 한 번에 최대 " + MAX_RANGE_SIZE + "개까지 조회할 수 있습니다.",
                    Map.of(REASON, "passage_range_too_large", "rangeSize", Integer.toString(range.size())));
        }
    }
}
