package yeobaek.backend.support.monitoring;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MonitoringSettings {

    private final String repository;
    private final String token;
    private final String ref;
    private final String environment;
    private final List<String> deploymentWorkflows;

    public MonitoringSettings(
            @Value("${MONITOR_GITHUB_REPOSITORY:woowacourse-teams/2026-yeobaek}") String repository,
            @Value("${MONITOR_GITHUB_TOKEN:}") String token,
            @Value("${MONITOR_WORKFLOW_REF:develop}") String ref,
            @Value("${MONITOR_ENVIRONMENT:dev}") String environment,
            @Value("${MONITOR_DEPLOY_WORKFLOWS:backend-dev.yml}") String deploymentWorkflows) {
        this.repository = repository;
        this.token = token;
        this.ref = ref;
        this.environment = environment;
        this.deploymentWorkflows = Arrays.stream(deploymentWorkflows.split(",")).map(String::trim).toList();
    }

    public void requireConfigured() {
        if (token.isBlank() || !repository.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")
                || !List.of("dev", "prod").contains(environment)
                || deploymentWorkflows.stream().anyMatch(name -> !name.matches("[A-Za-z0-9_-]+\\.ya?ml"))) {
            throw new IllegalStateException("배포 모니터링 GitHub 연결 설정이 필요합니다.");
        }
    }

    public String getRepository() {
        return repository;
    }

    public String getToken() {
        return token;
    }

    public String getRef() {
        return ref;
    }

    public String getEnvironment() {
        return environment;
    }

    public List<String> getDeploymentWorkflows() {
        return List.copyOf(deploymentWorkflows);
    }

    public String getDeploymentBranch() {
        return "dev".equals(environment) ? "develop" : "main";
    }
}
