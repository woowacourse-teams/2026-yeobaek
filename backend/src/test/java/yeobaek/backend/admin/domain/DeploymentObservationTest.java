package yeobaek.backend.admin.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DeploymentObservationTest {

    @Test
    @DisplayName("연결한 배포와 롤백이 모두 끝나면 마지막 완료부터 5분을 관찰한다")
    void waitsFiveMinutesAfterLastDeployment() {
        Instant deployed = Instant.parse("2026-11-11T00:01:00Z");
        Instant rolledBack = deployed.plusSeconds(120);
        var observation = new DeploymentObservation(List.of(
                new DeploymentProgress("completed", deployed, List.of()),
                new DeploymentProgress("completed", rolledBack, List.of())));

        assertThat(observation.endsAt()).contains(rolledBack.plusSeconds(300));
    }

    @Test
    @DisplayName("진행 중이거나 완료 시각을 모르는 실행이 있으면 자동 종료 시각을 정하지 않는다")
    void doesNotInferCompletionForUnknownOrOngoingRuns() {
        var finished = new DeploymentProgress("completed", Instant.parse("2026-11-11T00:01:00Z"), List.of());
        var ongoing = new DeploymentProgress("in_progress", null, List.of());
        var unknown = new DeploymentProgress("completed", null, List.of());

        assertThat(new DeploymentObservation(List.of(finished, ongoing)).endsAt()).isEmpty();
        assertThat(new DeploymentObservation(List.of(finished, unknown)).endsAt()).isEmpty();
        assertThat(new DeploymentObservation(List.of()).endsAt()).isEmpty();
    }
}
