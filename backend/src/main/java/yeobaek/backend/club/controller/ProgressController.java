package yeobaek.backend.club.controller;

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

@RestController
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    @PutMapping("/api/clubs/{clubId}/progress")
    public ProgressResponse updateProgress(@AuthMember Long memberId,
                                           @PathVariable Long clubId,
                                           @RequestBody ProgressUpdateRequest request) {
        return progressService.updateProgress(memberId, clubId, request.passageId());
    }

    @GetMapping("/api/members/me/last-reading")
    public ResponseEntity<LastReadingResponse> findLastReading(@AuthMember Long memberId) {
        return progressService.findLastReading(memberId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
