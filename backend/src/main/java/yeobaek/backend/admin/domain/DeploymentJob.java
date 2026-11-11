package yeobaek.backend.admin.domain;

import java.time.Instant;

public record DeploymentJob(String name, String status, Instant completedAt) {

    public boolean isRunning(String jobName) {
        return jobName.equals(name) && "in_progress".equals(status);
    }
}
