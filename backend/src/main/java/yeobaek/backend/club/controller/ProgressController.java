package yeobaek.backend.club.controller;

import static yeobaek.backend.support.LogField.ATTEMPT;
import static yeobaek.backend.support.LogField.CLUB_ID;
import static yeobaek.backend.support.LogField.MEMBER_ID;
import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.PHASE;
import static yeobaek.backend.support.LogField.SUCCESS;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import yeobaek.backend.auth.AuthMember;
import yeobaek.backend.club.dto.LastReadingResponse;
import yeobaek.backend.club.dto.ProgressResponse;
import yeobaek.backend.club.dto.ProgressUpdateRequest;
import yeobaek.backend.club.service.ProgressService;
import yeobaek.backend.support.analytics.AnalyticsEvent;
import yeobaek.backend.support.analytics.AnalyticsTracker;

@Tag(name = "읽기")
@SecurityRequirement(name = "memberId")
@RestController
@RequiredArgsConstructor
@Slf4j
public class ProgressController {

    private final ProgressService progressService;
    private final AnalyticsTracker analyticsTracker;

    @Operation(summary = "진도 갱신 (최근 열람 보고)",
            description = "항상 마지막 열람 본문으로 덮어쓴다. 앞부분 재열람 시 진도율은 후퇴한다 (PRD 3.4).")
    @PutMapping("/api/clubs/{clubId}/progress")
    public ProgressResponse updateProgress(@AuthMember Long memberId,
                                           @Parameter(description = "모임 ID") @PathVariable Long clubId,
                                           @Valid @RequestBody ProgressUpdateRequest request) {
        log.atInfo().addKeyValue(OPERATION, "progress.update").addKeyValue(PHASE, ATTEMPT)
                .addKeyValue(MEMBER_ID, memberId).addKeyValue(CLUB_ID, clubId)
                .addKeyValue("passageId", request.passageId()).log("진도 갱신 API 처리를 시작합니다.");
        ProgressResponse response = progressService.updateProgress(memberId, clubId, request.passageId());
        analyticsTracker.track(memberId, AnalyticsEvent.progressUpdate(
                clubId, request.passageId(), response.lastReadPassageSequence(), response.progressRate()));
        log.atInfo().addKeyValue(OPERATION, "progress.update").addKeyValue(PHASE, SUCCESS)
                .addKeyValue(MEMBER_ID, memberId).addKeyValue(CLUB_ID, clubId)
                .addKeyValue("progressRate", response.progressRate()).log("진도 갱신 API 처리를 완료했습니다.");
        return response;
    }

    @Operation(summary = "홈 — 마지막으로 읽던 책 조회",
            description = "전 모임 중 마지막으로 읽은 시간이 가장 최근인 모임. 읽기 기록이 없으면 204.")
    @GetMapping("/api/members/me/last-reading")
    public ResponseEntity<LastReadingResponse> findLastReading(@AuthMember Long memberId) {
        log.atInfo().addKeyValue(OPERATION, "progress.findLastReading").addKeyValue(PHASE, ATTEMPT)
                .addKeyValue(MEMBER_ID, memberId).log("최근 독서 API 처리를 시작합니다.");
        Optional<LastReadingResponse> lastReading = progressService.findLastReading(memberId);
        lastReading.ifPresentOrElse(
                response -> analyticsTracker.track(memberId, AnalyticsEvent.lastReadingView(
                        response.clubId(), response.book().bookId(),
                        response.lastReadPassageSequence(), response.progressRate())),
                () -> analyticsTracker.track(memberId, AnalyticsEvent.lastReadingView()));
        log.atInfo().addKeyValue(OPERATION, "progress.findLastReading").addKeyValue(PHASE, SUCCESS)
                .addKeyValue(MEMBER_ID, memberId).addKeyValue("found", lastReading.isPresent())
                .log("최근 독서 API 처리를 완료했습니다.");
        return lastReading
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
