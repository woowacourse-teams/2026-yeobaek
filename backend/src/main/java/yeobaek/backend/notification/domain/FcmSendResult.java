package yeobaek.backend.notification.domain;

import java.util.List;

public record FcmSendResult(int successCount, List<FcmSendFailure> failures) {

    public FcmSendResult {
        failures = List.copyOf(failures);
    }

    public int failureCount() {
        return failures.size();
    }

    public List<FcmSendFailure> retryableFailures() {
        return failures.stream()
                .filter(FcmSendFailure::isRetryable)
                .toList();
    }
}
