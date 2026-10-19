package yeobaek.backend.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FcmSendResultTest {

    @Test
    @DisplayName("원본 목록을 변경해도 실패 대상과 건수는 유지되고 반환 목록은 변경할 수 없다")
    void preserveFailureSnapshot() {
        var failure = new FcmSendFailure("c" + "a".repeat(21), "UNAVAILABLE", "UNAVAILABLE", 503, null);
        var failures = new ArrayList<>(List.of(failure));
        var result = new FcmSendResult(2, failures);

        failures.clear();

        assertThat(result.failures()).containsExactly(failure);
        assertThat(result.failureCount()).isEqualTo(1);
        assertThatThrownBy(() -> result.failures().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("재시도 후보를 조회해도 원래 실패 목록과 건수는 유지된다")
    void preserveFailuresWhenSelectingRetryableTargets() {
        var unavailable = new FcmSendFailure("c" + "a".repeat(21), "UNAVAILABLE", "UNAVAILABLE", 503, null);
        var unregistered = new FcmSendFailure("d" + "a".repeat(21), "UNREGISTERED", "NOT_FOUND", 404, null);
        var quotaExceeded = new FcmSendFailure("e" + "a".repeat(21), "QUOTA_EXCEEDED", "RESOURCE_EXHAUSTED", 429, "120");
        var result = new FcmSendResult(2, List.of(unavailable, unregistered, quotaExceeded));

        assertThat(result.retryableFailures()).containsExactly(unavailable, quotaExceeded);
        assertThat(result.failures()).containsExactly(unavailable, unregistered, quotaExceeded);
        assertThat(result.failureCount()).isEqualTo(3);
    }
}
