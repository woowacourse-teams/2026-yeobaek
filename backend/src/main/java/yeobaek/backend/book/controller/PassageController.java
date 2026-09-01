package yeobaek.backend.book.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import yeobaek.backend.auth.AuthMember;
import yeobaek.backend.book.dto.PassagesResponse;
import yeobaek.backend.book.service.PassageService;
import yeobaek.backend.support.analytics.AnalyticsEvent;
import yeobaek.backend.support.analytics.AnalyticsTracker;

@Tag(name = "읽기")
@SecurityRequirement(name = "memberId")
@RestController
@RequiredArgsConstructor
public class PassageController {

    private final PassageService passageService;
    private final AnalyticsTracker analyticsTracker;

    @Operation(summary = "본문 범위 조회",
            description = "전체 순서 기준 범위(양 끝 포함)의 본문을 조회한다. 범위는 최대 100개, 모임 미소속은 403.")
    @GetMapping("/api/clubs/{clubId}/passages")
    public PassagesResponse findPassages(@AuthMember Long memberId,
                                         @Parameter(description = "모임 ID") @PathVariable Long clubId,
                                         @Parameter(description = "시작 본문의 전체 순서 (1 이상)")
                                         @RequestParam int from,
                                         @Parameter(description = "끝 본문의 전체 순서 (양 끝 포함, to-from+1 ≤ 100)")
                                         @RequestParam int to) {
        PassagesResponse response = passageService.findPassages(memberId, clubId, from, to);
        analyticsTracker.track(memberId,
                AnalyticsEvent.passagesViewed(clubId, from, to, response.passages().size()));
        return response;
    }
}
