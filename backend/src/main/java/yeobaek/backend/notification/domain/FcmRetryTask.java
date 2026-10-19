package yeobaek.backend.notification.domain;

import java.util.List;
import java.util.Objects;

// 재시도 후보의 발송 정보를 보관한다. 생성만으로 예약하거나 발송하지 않는다.
public record FcmRetryTask(
        Long commentId,
        List<Long> registrationIds,
        String title,
        String body,
        FcmSendFailure failure
) {

    public FcmRetryTask {
        Objects.requireNonNull(commentId);
        Objects.requireNonNull(title);
        Objects.requireNonNull(body);
        Objects.requireNonNull(failure);
        Objects.requireNonNull(failure.fid());
        registrationIds = List.copyOf(registrationIds);
        if (registrationIds.isEmpty() || !failure.isRetryable()) {
            throw new IllegalArgumentException("발송 당시 등록과 재시도 가능한 실패가 필요합니다.");
        }
    }

    public String fid() {
        return failure.fid();
    }
}
