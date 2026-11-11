package yeobaek.backend.admin.domain;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class DeploymentProgress {

    private static final String COMPLETED = "completed";

    private final String status;
    private final Instant updatedAt;
    private final List<DeploymentJob> jobs;

    public DeploymentProgress(String status, Instant updatedAt, List<DeploymentJob> jobs) {
        this.status = status;
        this.updatedAt = updatedAt;
        this.jobs = List.copyOf(jobs);
    }

    public String phase() {
        if (COMPLETED.equals(status)) {
            return COMPLETED;
        }
        if (jobs.stream().anyMatch(job -> job.isRunning("deploy"))) {
            return "deploying";
        }
        if (jobs.stream().anyMatch(job -> job.isRunning("build-and-push"))) {
            return "building";
        }
        return "in_progress".equals(status) ? "running" : "waiting";
    }

    public Optional<Instant> completedAt() {
        if (!COMPLETED.equals(status)) {
            return Optional.empty();
        }

        return jobs.stream()
                .map(DeploymentJob::completedAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .or(() -> Optional.ofNullable(updatedAt));
    }
}
