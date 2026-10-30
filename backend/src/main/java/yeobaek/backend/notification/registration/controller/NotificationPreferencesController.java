package yeobaek.backend.notification.registration.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import yeobaek.backend.auth.AuthMember;
import yeobaek.backend.notification.registration.domain.FirebaseInstallationId;
import yeobaek.backend.notification.registration.dto.NotificationPreferencesResponse;
import yeobaek.backend.notification.registration.dto.NotificationPreferencesUpdateRequest;
import yeobaek.backend.notification.registration.service.NotificationPreferencesService;

@Tag(name = "알림 설정")
@SecurityRequirement(name = "memberId")
@RestController
@RequiredArgsConstructor
public class NotificationPreferencesController {
    private final NotificationPreferencesService preferencesService;

    @Operation(summary = "현재 회원의 설치별 알림 설정 조회")
    @GetMapping("/api/notifications/preferences")
    public NotificationPreferencesResponse find(@AuthMember Long memberId, @RequestParam String fid) {
        return preferencesService.find(memberId, new FirebaseInstallationId(fid));
    }

    @Operation(summary = "현재 회원의 설치별 알림 설정 부분 변경",
            description = "생략하거나 null인 항목은 유지해요. 전체 OFF는 유형별 설정을 변경하지 않아요.")
    @PatchMapping("/api/notifications/preferences")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@AuthMember Long memberId, @RequestParam String fid,
                       @RequestBody NotificationPreferencesUpdateRequest request) {
        preferencesService.update(memberId, new FirebaseInstallationId(fid), request);
    }
}
