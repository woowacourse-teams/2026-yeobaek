package yeobaek.backend.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FcmRetryTaskTest {

    private static final Instant CREATED_AT = Instant.parse("2026-10-19T00:00:00Z");

    private final FcmSendFailure failure = new FcmSendFailure(
            "c" + "a".repeat(21), "UNAVAILABLE", "UNAVAILABLE", 503, "120");

    @Test
    @DisplayName("원본 Map이 바뀌거나 추가 발송이 실패해도 처음의 화면 이동 정보를 유지한다")
    void preserveNavigationDataAcrossFailures() {
        Map<String, String> expected = Map.of(
                "type", "CLUB_COMMENT", "clubId", "5", "passageSequence", "3", "sentenceId", "13");
        var data = new HashMap<>(expected);
        var task = new FcmRetryTask(11L, List.of(1L), "제목", "내용", data, failure,
                CREATED_AT, CREATED_AT, 0, false);

        data.put("sentenceId", "999");
        var next = task.afterFailure(failure, CREATED_AT.plusSeconds(120), List.of(1L)).orElseThrow();

        assertThat(task.data()).isEqualTo(expected);
        assertThat(next.data()).isEqualTo(expected);
        assertThatThrownBy(() -> task.data().put("sentenceId", "999"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("등록 ID는 복사해 보관하므로 원본이나 반환값으로 발송 당시 대상을 바꿀 수 없다")
    void preserveRegistrationSnapshot() {
        var ids = new ArrayList<>(List.of(1L, 2L));
        var task = new FcmRetryTask(11L, ids, "제목", "내용", Map.of(), failure, CREATED_AT, CREATED_AT, 0, false);

        ids.clear();
        ids.add(3L);

        assertThat(task.registrationIds()).containsExactly(1L, 2L);
        assertThatThrownBy(() -> task.registrationIds().add(4L)).isInstanceOf(UnsupportedOperationException.class);
        assertThat(task.fid()).isEqualTo(failure.fid());
    }

    @Test
    @DisplayName("발송 당시 등록이 없으면 재시도 작업을 생성할 수 없다")
    void rejectEmptyRegistrationIds() {
        assertThatThrownBy(() -> new FcmRetryTask(11L, List.of(), "제목", "내용", Map.of(), failure, CREATED_AT, CREATED_AT, 0, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("재시도하지 않는 실패나 대상이 없는 호출 전체 실패로 작업을 생성할 수 없다")
    void rejectIneligibleFailure() {
        var permanent = new FcmSendFailure(failure.fid(), "INVALID_ARGUMENT", "INVALID_ARGUMENT", 400, null);
        var callFailure = new FcmSendFailure(null, "UNKNOWN", "DEADLINE_EXCEEDED", null, null);

        assertThatThrownBy(() -> new FcmRetryTask(11L, List.of(1L), "제목", "내용", Map.of(), permanent, CREATED_AT, CREATED_AT, 0, false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FcmRetryTask(11L, List.of(1L), "제목", "내용", Map.of(), callFailure, CREATED_AT, CREATED_AT, 0, false))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("다음 시각을 여러 번 계산해도 발송 이력은 바뀌지 않는다")
    void calculateWithoutConsumingRetry() {
        var task = new FcmRetryTask(11L, List.of(1L), "제목", "내용", Map.of(), failure,
                CREATED_AT, CREATED_AT, 0, false);

        assertThat(task.nextRetryAt(0)).contains(CREATED_AT.plusSeconds(120));
        assertThat(task.nextRetryAt(20)).contains(CREATED_AT.plusSeconds(132));
        assertThat(task.stopReason(20)).isEmpty();
        assertThat(task.retriesPerformed()).isZero();
        assertThat(task.uncertainRetryUsed()).isFalse();
    }

    @Test
    @DisplayName("작업이 가진 댓글 생성 시각과 이력으로 재시도 종료 여부를 판단한다")
    void stopUsingOriginalCreationTimeAndHistory() {
        var expired = new FcmRetryTask(11L, List.of(1L), "제목", "내용", Map.of(), failure,
                CREATED_AT, CREATED_AT.plusSeconds(1800), 0, false);
        var exhausted = new FcmRetryTask(11L, List.of(1L), "제목", "내용", Map.of(), failure,
                CREATED_AT, CREATED_AT.plusSeconds(120), 5, false);

        assertThat(expired.nextRetryAt(0)).isEmpty();
        assertThat(expired.stopReason(0)).contains("EXPIRED");
        assertThat(exhausted.nextRetryAt(0)).isEmpty();
        assertThat(exhausted.stopReason(0)).contains("RETRY_LIMIT_REACHED");
    }

    @Test
    @DisplayName("실제 발송 후 실패할 때만 횟수를 올리고 원래 댓글 생성 시각과 남은 등록을 유지한다")
    void advanceHistoryAfterActualAttempt() {
        var original = new FcmRetryTask(11L, List.of(1L, 2L), "제목", "내용", Map.of(), failure,
                CREATED_AT, CREATED_AT, 0, false);
        Instant observedAt = CREATED_AT.plusSeconds(120);

        var next = original.afterFailure(failure, observedAt, List.of(2L)).orElseThrow();

        assertThat(next.retriesPerformed()).isEqualTo(1);
        assertThat(next.registrationIds()).containsExactly(2L);
        assertThat(next.commentCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(next.observedAt()).isEqualTo(observedAt);
        assertThat(original.retriesPerformed()).isZero();
    }

    @Test
    @DisplayName("접수 불명확을 이유로 수행한 추가 발송 이력은 오류가 바뀌어도 유지한다")
    void consumeUncertainRetryOnlyWhenAttempted() {
        var uncertain = new FcmSendFailure(failure.fid(), "UNKNOWN", "DEADLINE_EXCEEDED", null, null);
        var original = new FcmRetryTask(11L, List.of(1L), "제목", "내용", Map.of(), failure,
                CREATED_AT, CREATED_AT, 0, false);

        var first = original.afterFailure(uncertain, CREATED_AT.plusSeconds(60), List.of(1L)).orElseThrow();
        var second = first.afterFailure(failure, CREATED_AT.plusSeconds(120), List.of(1L)).orElseThrow();
        var third = second.afterFailure(uncertain, CREATED_AT.plusSeconds(240), List.of(1L)).orElseThrow();

        assertThat(first.uncertainRetryUsed()).isFalse();
        assertThat(second.uncertainRetryUsed()).isTrue();
        assertThat(third.uncertainRetryUsed()).isTrue();
        assertThat(third.stopReason(0)).contains("UNCERTAIN_RETRY_LIMIT_REACHED");
    }

    @Test
    @DisplayName("영구 실패에는 후속 작업을 만들지 않고 다른 FID의 실패를 연결하지 않는다")
    void rejectUnrelatedFailureAndStopPermanentFailure() {
        var original = new FcmRetryTask(11L, List.of(1L), "제목", "내용", Map.of(), failure,
                CREATED_AT, CREATED_AT, 0, false);
        var invalid = new FcmSendFailure(failure.fid(), "INVALID_ARGUMENT", "INVALID_ARGUMENT", 400, null);
        var otherTarget = new FcmSendFailure("d" + "a".repeat(21), "INTERNAL", "INTERNAL", 500, null);

        assertThat(original.afterFailure(invalid, CREATED_AT.plusSeconds(60), List.of(1L))).isEmpty();
        assertThatThrownBy(() -> original.afterFailure(otherTarget, CREATED_AT.plusSeconds(60), List.of(1L)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(original.isExpired(CREATED_AT.plusSeconds(1800))).isTrue();
    }

    @Test
    @DisplayName("호출 전체의 접수 불명확은 개별 실패 FID를 채우지 않고 추가 시도 대상과 이력을 보관한다")
    void retainCallScopeWithExplicitRetryTarget() {
        var callFailure = new FcmSendFailure(null, "UNKNOWN", "DEADLINE_EXCEEDED", null, null);
        var task = new FcmRetryTask(11L, List.of(1L), "제목", "내용", Map.of(), callFailure,
                CREATED_AT, CREATED_AT, 0, false, failure.fid());

        var next = task.afterFailure(callFailure, CREATED_AT.plusSeconds(60), List.of(1L)).orElseThrow();

        assertThat(task.isCallFailure()).isTrue();
        assertThat(task.failure().fid()).isNull();
        assertThat(task.fid()).isEqualTo(failure.fid());
        assertThat(next.fid()).isEqualTo(failure.fid());
        assertThat(next.stopReason(0)).contains("UNCERTAIN_RETRY_LIMIT_REACHED");
    }
}
