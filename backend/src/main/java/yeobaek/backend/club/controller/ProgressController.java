package yeobaek.backend.club.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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

@Tag(name = "읽기")
@SecurityRequirement(name = "memberId")
@RestController
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    @Operation(summary = "진도 갱신 (최근 열람 보고)",
            description = "항상 마지막 열람 본문으로 덮어쓴다. 앞부분 재열람 시 진도율은 후퇴한다 (PRD 3.4).")
    @PutMapping("/api/clubs/{clubId}/progress")
    public ProgressResponse updateProgress(@AuthMember Long memberId,
                                           @Parameter(description = "모임 ID") @PathVariable Long clubId,
                                           @RequestBody ProgressUpdateRequest request) {
        return progressService.updateProgress(memberId, clubId, request.passageId());
    }

    @Operation(summary = "홈 — 마지막으로 읽던 책 조회",
            description = "전 모임 중 마지막으로 읽은 시간이 가장 최근인 모임. 읽기 기록이 없으면 204.")
    @GetMapping("/api/members/me/last-reading")
    public ResponseEntity<LastReadingResponse> findLastReading(@AuthMember Long memberId) {
        return progressService.findLastReading(memberId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
