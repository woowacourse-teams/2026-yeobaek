package yeobaek.backend.web.v1;

import static yeobaek.backend.support.LogField.CLUB_ID;
import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.RESULT;
import static yeobaek.backend.support.LogField.SUCCESS;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.application.content.ContentReadingQueryService;
import yeobaek.backend.web.book.dto.PassageResponse;
import yeobaek.backend.web.book.dto.PassagesResponse;
import yeobaek.backend.web.book.dto.SentenceResponse;
import yeobaek.backend.collaboration.api.binding.SpaceContentBindingApi;
import yeobaek.backend.content.api.location.ContentLegacyLocationQueryApi;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.space.api.club.ClubApi;
import yeobaek.backend.space.api.club.ClubResponse;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.shared.exception.NotFoundException;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PassageService {

    private static final int MAX_RANGE_SIZE = 100;

    private final ClubApi clubs;
    private final SpaceContentBindingApi bindingApi;
    private final ContentReadingQueryService queries;
    private final ContentLegacyLocationQueryApi locations;

    public PassagesResponse findPassages(Long memberId, Long clubId, int from, int to) {
        log.atInfo().addKeyValue(OPERATION, "passage.findPassages")
                .addKeyValue(CLUB_ID, clubId)
                .addKeyValue("from", from).addKeyValue("to", to).log("본문 범위를 조회합니다.");
        ClubResponse club = clubs.findById(clubId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.CLUB_NOT_FOUND,
                        "본문을 조회할 모임이 존재하지 않습니다: clubId=" + clubId,
                        Map.of(CLUB_ID, clubId.toString())));
        SpaceId spaceId = club.id();
        ContentId contentId = requireBoundContent(spaceId, clubId);
        var body = queries.findBody(new MemberId(memberId), spaceId, contentId, from, to);
        var sentenceInfo = locations.findSentenceInfo(body.passages().stream()
                .flatMap(passage -> passage.sentences().stream())
                .map(ContentReadingQueryService.SentenceResult::locationId).toList()).stream()
                .collect(java.util.stream.Collectors.toMap(info -> info.locationId(), info -> info));
        var response = new PassagesResponse(body.passages().stream()
                .map(passage -> new PassageResponse(
                        locations.findByLocation(passage.locationId()).orElseThrow().passageId(),
                        passage.sequence(), passage.sectionId(), passage.sentences().stream()
                        .map(sentence -> new SentenceResponse(sentenceInfo.get(sentence.locationId()).sentenceId(),
                                sentence.sequence(), sentence.content(), sentence.commentCount()))
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

}
