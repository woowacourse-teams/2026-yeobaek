package yeobaek.backend.notification.registration.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import yeobaek.backend.auth.AuthMember;
import yeobaek.backend.notification.registration.domain.FirebaseInstallationId;
import yeobaek.backend.notification.registration.dto.NotificationClubPreferenceResponse;
import yeobaek.backend.notification.registration.dto.NotificationClubPreferenceUpdateRequest;
import yeobaek.backend.notification.registration.service.NotificationClubPreferencesService;

@Tag(name = "알림 설정")
@SecurityRequirement(name = "memberId")
@RestController
@RequiredArgsConstructor
public class NotificationClubPreferencesController {
    private final NotificationClubPreferencesService preferencesService;

    @Operation(summary = "현재 기기의 모임별 알림 설정 조회",
            description = "모임별 알림 설정의 기본값은 ON이에요. 응답은 해당 모임의 설정이며, "
                    + "실제 수신에는 전체 알림과 해당 유형 알림도 켜져 있어야 해요.")
    @GetMapping("/api/clubs/{clubId}/notifications/preferences")
    public NotificationClubPreferenceResponse find(@AuthMember Long memberId, @PathVariable Long clubId,
                                                   @RequestParam String fid) {
        return preferencesService.find(memberId, new FirebaseInstallationId(fid), clubId);
    }

    @Operation(summary = "현재 기기의 모임별 알림 설정 변경",
            description = "참여 중인 모임만 변경할 수 있어요. OFF는 해당 기기의 모임 가입·댓글 알림에 적용돼요.")
    @PatchMapping("/api/clubs/{clubId}/notifications/preferences")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@AuthMember Long memberId, @PathVariable Long clubId, @RequestParam String fid,
                       @Valid @RequestBody NotificationClubPreferenceUpdateRequest request) {
        preferencesService.update(memberId, new FirebaseInstallationId(fid), clubId, request);
    }
}
