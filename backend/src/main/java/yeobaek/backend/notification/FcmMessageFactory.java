package yeobaek.backend.notification;

import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FcmMessageFactory {
    public MulticastMessage createMulticastMessage(List<String> fids, String title, String body) {
        return MulticastMessage.builder()
                .addAllFids(fids)
                .setNotification(createNotification(title, body))
                .build();
    }

    private Notification createNotification(String title, String body) {
        return Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();
    }
}
