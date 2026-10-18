package yeobaek.backend.notification;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FcmPushSender {
    private final FirebaseMessaging messaging;

    public BatchResponse send(MulticastMessage message) throws FirebaseMessagingException {
        return messaging.sendEachForMulticast(message);
    }
}
