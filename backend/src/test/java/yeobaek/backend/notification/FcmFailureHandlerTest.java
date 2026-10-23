package yeobaek.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import ch.qos.logback.classic.Level;
import com.google.firebase.ErrorCode;
import com.google.firebase.messaging.FirebaseMessagingException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.DataAccessResourceFailureException;
import yeobaek.backend.notification.domain.FcmSendFailure;
import yeobaek.backend.notification.domain.FcmSendResult;
import yeobaek.backend.notification.registration.domain.FirebaseInstallationId;
import yeobaek.backend.notification.registration.domain.NotificationRegistrations;
import yeobaek.backend.notification.registration.service.NotificationRegistrationService;
import yeobaek.backend.support.LogCapture;

class FcmFailureHandlerTest {

    private static final String FID = "c" + "a".repeat(21);
    private final NotificationRegistrationService registrationService = mock(NotificationRegistrationService.class);
    private final FcmFailureHandler handler = new FcmFailureHandler(registrationService);
    private final NotificationRegistrations snapshot = new NotificationRegistrations(List.of());

    @ParameterizedTest
    @CsvSource(value = {
            "INVALID_ARGUMENT,INVALID_ARGUMENT,400",
            "SENDER_ID_MISMATCH,PERMISSION_DENIED,403",
            "THIRD_PARTY_AUTH_ERROR,UNAUTHENTICATED,401",
            "UNKNOWN,UNAUTHENTICATED,null",
            "UNKNOWN,PERMISSION_DENIED,null",
            "UNKNOWN,NOT_FOUND,404",
            "UNKNOWN,UNKNOWN,null"
    }, nullValues = "null")
    @DisplayName("요청·인증·미확인 오류는 등록을 삭제하지 않고 원인 정보와 ERROR 로그를 남긴다")
    void logPermanentFailure(String detail, String sdk, Integer status) {
        var failure = new FcmSendFailure(FID, detail, sdk, status, null);

        try (var logs = new LogCapture(FcmFailureHandler.class.getName())) {
            handler.handle(new FcmSendResult(0, List.of(failure)), snapshot);

            var event = logs.event("notification.fcm.logFailure", "failure");
            assertThat(event.getLevel()).isEqualTo(Level.ERROR);
            assertThat(List.of(logs.field(event, "errorCode"), logs.field(event, "sdkErrorCode")))
                    .containsExactly(detail, sdk);
            assertThat(logs.field(event, "retryDecision")).isEqualTo("none");
            assertThat(logs.structuredText()).doesNotContain(FID);
        }
        verifyNoInteractions(registrationService);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "INTERNAL,INTERNAL,500,false",
            "UNAVAILABLE,UNAVAILABLE,503,false",
            "QUOTA_EXCEEDED,RESOURCE_EXHAUSTED,429,false",
            "UNKNOWN,DEADLINE_EXCEEDED,null,true",
            "UNKNOWN,UNAVAILABLE,null,true"
    }, nullValues = "null")
    @DisplayName("일시 오류와 접수 불명확은 WARN으로 후보만 기록한다")
    void logRetryCandidate(String detail, String sdk, Integer status, boolean uncertain) {
        var failure = new FcmSendFailure(FID, detail, sdk, status, "120");

        try (var logs = new LogCapture(FcmFailureHandler.class.getName())) {
            handler.handle(new FcmSendResult(0, List.of(failure)), snapshot);

            var event = logs.event("notification.fcm.logFailure", "failure");
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
            assertThat(logs.field(event, "retryDecision")).isEqualTo("candidate");
            assertThat(logs.field(event, "receiptUncertain")).isEqualTo(uncertain);
            assertThat(event.getFormattedMessage()).doesNotContain("예약했습니다", "발송했습니다");
        }
        verifyNoInteractions(registrationService);
    }

    @Test
    @DisplayName("등록 해제된 FID는 발송 당시 스냅샷으로 삭제하고 실제 삭제 건수를 기록한다")
    void removeOnlyUnregisteredTarget() {
        var unregistered = new FcmSendFailure(FID, "UNREGISTERED", "NOT_FOUND", 404, null);
        var invalid = new FcmSendFailure("d" + "a".repeat(21), "INVALID_ARGUMENT", "INVALID_ARGUMENT", 400, null);
        var fid = new FirebaseInstallationId(FID);
        given(registrationService.deleteUnregistered(snapshot, fid)).willReturn(2);

        try (var logs = new LogCapture(FcmFailureHandler.class.getName())) {
            handler.handle(new FcmSendResult(0, List.of(unregistered, invalid)), snapshot);

            var event = logs.event("notification.registration.deleteRegistration", "success");
            assertThat(event.getLevel()).isEqualTo(Level.INFO);
            assertThat(logs.field(event, "deletedCount")).isEqualTo(2);
        }
        verify(registrationService).deleteUnregistered(snapshot, fid);
        verify(registrationService, never()).deleteUnregistered(snapshot, new FirebaseInstallationId(invalid.fid()));
    }

    @Test
    @DisplayName("한 등록의 삭제가 실패해도 다음 실패를 처리하고 완료 로그를 남기지 않는다")
    void continueAfterCleanupFailure() {
        var first = new FcmSendFailure(FID, "UNREGISTERED", "NOT_FOUND", 404, null);
        var second = new FcmSendFailure("d" + "a".repeat(21), "UNREGISTERED", "NOT_FOUND", 404, null);
        given(registrationService.deleteUnregistered(snapshot, new FirebaseInstallationId(FID)))
                .willThrow(new DataAccessResourceFailureException("민감한 SQL " + FID));

        try (var logs = new LogCapture(FcmFailureHandler.class.getName())) {
            handler.handle(new FcmSendResult(0, List.of(first, second)), snapshot);

            assertThat(logs.event("notification.registration.deleteRegistration", "success").getLevel()).isEqualTo(Level.INFO);
            assertThat(logs.event("notification.registration.deleteRegistration", "failure").getLevel()).isEqualTo(Level.ERROR);
            assertThat(logs.structuredText()).doesNotContain(FID, "민감한 SQL");
        }
        verify(registrationService).deleteUnregistered(snapshot, new FirebaseInstallationId(second.fid()));
    }

    @Test
    @DisplayName("응답 없는 호출 전체의 타임아웃은 개별 확정 실패 대신 호출 범위의 접수 불명확으로 기록한다")
    void logWholeCallAsUncertain() {
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        given(exception.getErrorCode()).willReturn(ErrorCode.DEADLINE_EXCEEDED);

        try (var logs = new LogCapture(FcmFailureHandler.class.getName())) {
            handler.handleCallFailure(11L, exception);

            var event = logs.event("notification.fcm.logFailure", "failure");
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
            assertThat(logs.field(event, "scope")).isEqualTo("call");
            assertThat(logs.field(event, "receiptUncertain")).isEqualTo(true);
            assertThat(logs.field(event, "commentId")).isEqualTo(11L);
        }
        verifyNoInteractions(registrationService);
    }
}
