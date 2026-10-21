package yeobaek.backend.notification;

import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.RESULT;
import static yeobaek.backend.support.LogField.SUCCESS;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import yeobaek.backend.notification.domain.FcmSendFailure;
import yeobaek.backend.notification.domain.FcmSendResult;

@Component
@RequiredArgsConstructor
@Slf4j
public class FcmPushSender {
    private final FirebaseMessaging messaging;
    private final FcmMessageFactory messageFactory;

    public FcmSendResult send(List<String> orderedFids, String title, String body, Map<String, String> data)
            throws FirebaseMessagingException {
        MulticastMessage message = messageFactory.createMulticastMessage(orderedFids, title, body, data);
        log.atInfo().addKeyValue(OPERATION, "notification.fcm.send").log("FCM 전송을 요청합니다.");
        BatchResponse response = messaging.sendEachForMulticast(message);
        FcmSendResult result = toResult(orderedFids, response);
        int successCount = result.successCount();
        int failureCount = result.failureCount();
        var event = failureCount == 0 ? log.atInfo() : log.atWarn();
        event.addKeyValue(OPERATION, "notification.fcm.send")
                .addKeyValue(RESULT, failureCount == 0 ? SUCCESS : "failure")
                .addKeyValue("successCount", successCount)
                .addKeyValue("failureCount", failureCount)
                .log("FCM 접수 결과를 기록합니다.");
        return result;
    }

    private FcmSendResult toResult(List<String> orderedFids, BatchResponse response) {
        List<SendResponse> outcomes = response.getResponses();
        if (orderedFids.size() != outcomes.size()) {
            throw new IllegalStateException("FCM 요청 대상 수와 응답 수가 일치하지 않습니다.");
        }

        List<FcmSendFailure> failures = new ArrayList<>();
        for (int index = 0; index < outcomes.size(); index++) {
            SendResponse outcome = outcomes.get(index);
            if (!outcome.isSuccessful()) {
                failures.add(FcmSendFailure.from(orderedFids.get(index), outcome.getException()));
            }
        }
        return new FcmSendResult(response.getSuccessCount(), failures);
    }
}
