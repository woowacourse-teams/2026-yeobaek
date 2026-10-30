package yeobaek.backend.notification.registration.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import yeobaek.backend.auth.AuthMember;
import yeobaek.backend.notification.registration.dto.NotificationRegistrationRequest;
import yeobaek.backend.notification.registration.service.NotificationRegistrationService;

@RestController
@RequiredArgsConstructor
public class NotificationRegistrationController {
    private final NotificationRegistrationService notificationRegistrationService;

    @PostMapping("/api/notifications/registrations")
    public void register(@AuthMember Long memberId, @RequestBody NotificationRegistrationRequest request) {
        notificationRegistrationService.register(memberId, request);
    }
}
