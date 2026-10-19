package yeobaek.backend.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FcmRetryTaskTest {

    private final FcmSendFailure failure = new FcmSendFailure(
            "c" + "a".repeat(21), "UNAVAILABLE", "UNAVAILABLE", 503, "120");

    @Test
    @DisplayName("등록 ID는 복사해 보관하므로 원본이나 반환값으로 발송 당시 대상을 바꿀 수 없다")
    void preserveRegistrationSnapshot() {
        var ids = new ArrayList<>(List.of(1L, 2L));
        var task = new FcmRetryTask(11L, ids, "제목", "내용", failure);

        ids.clear();
        ids.add(3L);

        assertThat(task.registrationIds()).containsExactly(1L, 2L);
        assertThatThrownBy(() -> task.registrationIds().add(4L)).isInstanceOf(UnsupportedOperationException.class);
        assertThat(task.fid()).isEqualTo(failure.fid());
    }

    @Test
    @DisplayName("발송 당시 등록이 없으면 재시도 작업을 생성할 수 없다")
    void rejectEmptyRegistrationIds() {
        assertThatThrownBy(() -> new FcmRetryTask(11L, List.of(), "제목", "내용", failure))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("재시도하지 않는 실패나 대상이 없는 호출 전체 실패로 작업을 생성할 수 없다")
    void rejectIneligibleFailure() {
        var permanent = new FcmSendFailure(failure.fid(), "INVALID_ARGUMENT", "INVALID_ARGUMENT", 400, null);
        var callFailure = new FcmSendFailure(null, "UNKNOWN", "DEADLINE_EXCEEDED", null, null);

        assertThatThrownBy(() -> new FcmRetryTask(11L, List.of(1L), "제목", "내용", permanent))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FcmRetryTask(11L, List.of(1L), "제목", "내용", callFailure))
                .isInstanceOf(NullPointerException.class);
    }
}
