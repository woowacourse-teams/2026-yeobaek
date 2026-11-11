package yeobaek.backend.admin.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DeploymentProgressTest {

    @ParameterizedTest
    @CsvSource({
            "queued, deploy, queued, waiting",
            "in_progress, build-and-push, in_progress, building",
            "in_progress, deploy, in_progress, deploying",
            "in_progress, verify, in_progress, running",
            "completed, deploy, completed, completed"
    })
    @DisplayName("실행 상태와 작업 이름으로 관리자 화면의 배포 단계를 판단한다")
    void identifiesPhase(String status, String jobName, String jobStatus, String expected) {
        var progress = new DeploymentProgress(status, null, List.of(new DeploymentJob(jobName, jobStatus, null)));

        assertThat(progress.phase()).isEqualTo(expected);
    }

    @Test
    @DisplayName("실행이 진행 중이면 일부 작업이 끝났어도 배포 완료 시각은 없다")
    void unfinishedRunHasNoCompletion() {
        Instant finishedJob = Instant.parse("2026-11-11T00:01:00Z");
        var progress = new DeploymentProgress("in_progress", finishedJob,
                List.of(new DeploymentJob("build-and-push", "completed", finishedJob)));

        assertThat(progress.completedAt()).isEmpty();
    }

    @Test
    @DisplayName("배포 완료 시각은 마지막 작업 종료 시각이며 작업 기록이 없으면 실행 갱신 시각을 사용한다")
    void usesLatestJobCompletionBeforeRunUpdate() {
        Instant first = Instant.parse("2026-11-11T00:01:00Z");
        Instant last = first.plusSeconds(60);
        Instant updated = last.plusSeconds(30);
        var progress = new DeploymentProgress("completed", updated, List.of(
                new DeploymentJob("build-and-push", "completed", first),
                new DeploymentJob("deploy", "completed", last),
                new DeploymentJob("skipped", "completed", null)));

        assertThat(progress.completedAt()).contains(last);
        assertThat(new DeploymentProgress("completed", updated, List.of()).completedAt()).contains(updated);
    }
}
