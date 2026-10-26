package yeobaek.backend.notification.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

// 재시도 후보의 발송 정보를 보관한다. 생성만으로 예약하거나 발송하지 않는다.
public record FcmRetryTask(
        Long commentId,
        List<Long> registrationIds,
        String title,
        String body,
        Map<String, String> data,
        FcmSendFailure failure,
        Instant commentCreatedAt,
        Instant observedAt,
        int retriesPerformed,
        boolean uncertainRetryUsed,
        String targetFid
) {

    public FcmRetryTask(Long commentId, List<Long> registrationIds, String title, String body,
                        Map<String, String> data, FcmSendFailure failure, Instant commentCreatedAt, Instant observedAt,
                        int retriesPerformed, boolean uncertainRetryUsed) {
        this(commentId, registrationIds, title, body, data, failure, commentCreatedAt, observedAt,
                retriesPerformed, uncertainRetryUsed, failure.fid());
    }

    public FcmRetryTask {
        Objects.requireNonNull(commentId);
        Objects.requireNonNull(title);
        Objects.requireNonNull(body);
        Objects.requireNonNull(failure);
        Objects.requireNonNull(targetFid);
        Objects.requireNonNull(commentCreatedAt);
        Objects.requireNonNull(observedAt);
        registrationIds = List.copyOf(registrationIds);
        data = Map.copyOf(data);
        if (registrationIds.isEmpty() || !failure.isRetryable()) {
            throw new IllegalArgumentException("발송 당시 등록과 재시도 가능한 실패가 필요합니다.");
        }
        if (retriesPerformed < 0 || observedAt.isBefore(commentCreatedAt)) {
            throw new IllegalArgumentException("재시도 이력과 댓글 생성·결과 확인 시각을 확인하세요.");
        }
        if (failure.fid() != null && !targetFid.equals(failure.fid())) {
            throw new IllegalArgumentException("실패 응답과 재시도 대상 FID가 일치해야 합니다.");
        }
    }

    public String fid() {
        return targetFid;
    }

    public boolean isCallFailure() {
        return failure.fid() == null;
    }

    public Optional<Instant> nextRetryAt(int jitterPercent) {
        return failure.nextRetryAt(commentCreatedAt, observedAt, retriesPerformed, uncertainRetryUsed, jitterPercent);
    }

    public Optional<String> stopReason(int jitterPercent) {
        return failure.retryStopReason(commentCreatedAt, observedAt, retriesPerformed, uncertainRetryUsed, jitterPercent);
    }

    public boolean isExpired(Instant now) {
        return failure.isExpired(commentCreatedAt, now);
    }

    // 예약·제출이 아니라 실제 추가 발송을 마친 뒤 호출한다.
    public Optional<FcmRetryTask> afterFailure(
            FcmSendFailure nextFailure, Instant nextObservedAt, List<Long> remainingRegistrationIds) {
        if (nextFailure.fid() != null && !fid().equals(nextFailure.fid())) {
            throw new IllegalArgumentException("재시도 대상 FID는 바뀔 수 없습니다.");
        }
        if (!nextFailure.isRetryable()) {
            return Optional.empty();
        }
        return Optional.of(new FcmRetryTask(commentId, remainingRegistrationIds, title, body, data, nextFailure,
                commentCreatedAt, nextObservedAt, retriesPerformed + 1,
                uncertainRetryUsed || failure.isReceiptUncertain(), targetFid));
    }
}
