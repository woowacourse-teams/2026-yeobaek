package yeobaek.backend.notification;

import static yeobaek.backend.support.LogField.COMMENT_ID;
import static yeobaek.backend.support.LogField.CLUB_ID;
import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.RESULT;

import com.google.firebase.messaging.FirebaseMessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import yeobaek.backend.notification.domain.FcmSendFailure;
import yeobaek.backend.notification.domain.FcmSendResult;
import yeobaek.backend.notification.domain.NotificationType;
import yeobaek.backend.notification.registration.domain.FirebaseInstallationId;
import yeobaek.backend.notification.registration.domain.NotificationRegistrations;
import yeobaek.backend.notification.registration.service.NotificationRegistrationService;

@Component
@RequiredArgsConstructor
@Slf4j
public class FcmFailureHandler {

    private final NotificationRegistrationService registrationService;

    public void handle(FcmSendResult result, NotificationRegistrations sentRegistrations) {
        for (FcmSendFailure failure : result.failures()) {
            if (failure.isUnregistered()) {
                deleteRegistration(failure, sentRegistrations);
            } else {
                logFailure(failure, null, null, "target");
            }
        }
    }

    public void handleCallFailure(NotificationType type, Long sourceId, FirebaseMessagingException exception) {
        // BatchResponse가 없으므로 개별 FID의 확정 실패 결과를 만들지 않는다.
        FcmSendFailure failure = FcmSendFailure.from(null, exception);
        logFailure(failure, type, sourceId, "call");
    }

    public void handleInvalidRequest(NotificationType type, Long sourceId) {
        log.atError().addKeyValue(OPERATION, "notification.fcm.handleInvalidRequest")
                .addKeyValue(RESULT, "failure").addKeyValue(type == NotificationType.CLUB_JOIN ? CLUB_ID : COMMENT_ID, sourceId)
                .addKeyValue("notificationType", type == null ? null : type.name())
                .addKeyValue("handlingCode", "INVALID_ARGUMENT")
                .addKeyValue("retryDecision", "none")
                .log("FCM 요청을 생성할 수 없습니다. 대상과 메시지 필수값을 확인하세요.");
    }

    private void deleteRegistration(FcmSendFailure failure, NotificationRegistrations sentRegistrations) {
        log.atInfo().addKeyValue(OPERATION, "notification.registration.deleteRegistration")
                .log("FCM에서 해제된 대상의 서버 등록 정보를 정리합니다.");
        try {
            int deletedCount = registrationService.deleteUnregistered(
                    sentRegistrations, new FirebaseInstallationId(failure.fid()));
            log.atInfo().addKeyValue(OPERATION, "notification.registration.deleteRegistration")
                    .addKeyValue(RESULT, "success").addKeyValue("deletedCount", deletedCount)
                    .log("발송 당시 등록 정보 중 FCM에서 해제된 등록의 정리를 완료했습니다.");
        } catch (DataAccessException exception) {
            log.atError().addKeyValue(OPERATION, "notification.registration.deleteRegistration")
                    .addKeyValue(RESULT, "failure").addKeyValue("errorCode", failure.errorCode())
                    .addKeyValue("exceptionType", exception.getClass().getSimpleName())
                    .log("FCM에서 해제된 등록 정보를 삭제하지 못했습니다.");
        }
    }

    private void logFailure(FcmSendFailure failure, NotificationType type, Long sourceId, String scope) {
        var event = failure.isRetryable() ? log.atWarn() : log.atError();
        event.addKeyValue(OPERATION, "notification.fcm.logFailure").addKeyValue(RESULT, "failure")
                .addKeyValue(type == NotificationType.CLUB_JOIN ? CLUB_ID : COMMENT_ID, sourceId)
                .addKeyValue("notificationType", type == null ? null : type.name()).addKeyValue("scope", scope)
                .addKeyValue("errorCode", failure.errorCode()).addKeyValue("sdkErrorCode", failure.sdkErrorCode())
                .addKeyValue("httpStatus", failure.httpStatus()).addKeyValue("handlingCode", failure.handlingCode())
                .addKeyValue("retryDecision", failure.isRetryable() ? "candidate" : "none")
                .addKeyValue("receiptUncertain", failure.isReceiptUncertain())
                .addKeyValue("retryAfterPresent", failure.retryAfter() != null)
                .addKeyValue("retryAfterInvalid", failure.hasInvalidRetryAfter())
                .log(guidance(failure));
    }

    private String guidance(FcmSendFailure failure) {
        return switch (failure.handlingCode()) {
            case "INVALID_ARGUMENT" -> "FCM 요청값이 유효하지 않습니다. 대상과 메시지 필드를 확인하세요.";
            case "SENDER_ID_MISMATCH" -> "FCM 발신 프로젝트가 대상과 일치하지 않습니다. 앱과 서버 설정을 확인하세요.";
            case "UNAUTHENTICATED" -> "FCM 서버 인증에 실패했습니다. 실행 환경의 인증 정보를 확인하세요.";
            case "PERMISSION_DENIED" -> "FCM 발송 권한이 없습니다. 실행 계정과 프로젝트 권한을 확인하세요.";
            case "THIRD_PARTY_AUTH_ERROR" -> "플랫폼 푸시 인증에 실패했습니다. APNs 또는 Web Push 인증 정보를 확인하세요.";
            case "INTERNAL" -> "FCM 내부 오류를 추가 재시도 후보로 분류했습니다.";
            case "QUOTA_EXCEEDED" -> "FCM 전송량 제한입니다. 재시도 후보이며 대기 시간과 발송 속도 조절이 필요합니다.";
            case "UNAVAILABLE" -> "FCM 일시 장애를 추가 재시도 후보로 분류했습니다.";
            case "RECEIPT_UNCERTAIN" -> "FCM 접수 여부가 불명확합니다. 중복 가능성이 있는 1회 추가 시도 후보입니다.";
            case "NOT_FOUND" -> "FCM 요청 대상을 찾지 못했습니다. 등록 해제를 단정하지 말고 요청 경로와 설정을 확인하세요.";
            default -> "FCM 실패 원인을 확정할 수 없습니다. 자동 조치 없이 오류 코드와 HTTP 상태를 확인하세요.";
        };
    }
}
