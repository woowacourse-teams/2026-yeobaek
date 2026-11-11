package yeobaek.backend.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import yeobaek.backend.admin.dto.MonitoringLinkRequest;
import yeobaek.backend.support.LogCapture;
import yeobaek.backend.support.monitoring.MonitoringGitHubClient;
import yeobaek.backend.support.monitoring.MonitoringSettings;

class DeploymentMonitoringServiceTest {

    private static final String SESSION = "11111111-1111-1111-1111-111111111111";
    private final ObjectMapper mapper = new ObjectMapper();
    private final MonitoringGitHubClient github = mock(MonitoringGitHubClient.class);
    private final DeploymentMonitoringService service = new DeploymentMonitoringService(github,
            new MonitoringSettings("owner/repo", "test-token", "develop", "dev", "backend-dev.yml"));

    @Test
    @DisplayName("진행 중인 측정이 있으면 시작 요청을 중복 전송하지 않는다")
    void returnsExistingMeasurementInsteadOfDispatchingDuplicate() {
        when(github.pages(anyString(), anyString())).thenReturn(List.of(measurement("in_progress")));

        assertThat(service.start().session()).isEqualTo(SESSION);
        verify(github, never()).post(anyString(), any());
    }

    @Test
    @DisplayName("새 측정 UUID로 설정된 워크플로에 시작 명령을 전달한다")
    void startsOnlyConfiguredWorkflowWithFreshSession() {
        when(github.pages(anyString(), anyString())).thenReturn(List.of());

        var started = service.start();
        assertThat(started.session()).matches("[a-f0-9-]{36}");
        verify(github).post("/actions/workflows/deployment-monitor.yml/dispatches", Map.of("ref", "develop",
                "inputs", Map.of("environment", "dev", "session", started.session(), "command", "start")));
    }

    @Test
    @DisplayName("실행 취소 대신 측정 종료 명령을 전달한다")
    void stopDispatchesCommandRatherThanCancellingRunner() {
        when(github.pages(anyString(), anyString())).thenReturn(List.of(measurement("in_progress")));

        service.stop(SESSION);

        verify(github).post("/actions/workflows/deployment-monitor.yml/dispatches", Map.of("ref", "develop",
                "inputs", Map.of("environment", "dev", "session", SESSION, "command", "stop", "measurement_run_id", "42")));
    }

    @Test
    @DisplayName("이미 끝난 측정에는 종료 명령을 전달하지 않는다")
    void rejectsStopAfterCompletion() {
        when(github.pages(anyString(), anyString())).thenReturn(List.of(measurement("completed")));
        assertThatThrownBy(() -> service.stop(SESSION)).isInstanceOf(IllegalArgumentException.class);
        verify(github, never()).post(anyString(), any());
    }

    @Test
    @DisplayName("개발 측정에 운영 배포를 연결하지 않는다")
    void rejectsProductionDeploymentLinkedToDevelopmentMeasurement() {
        when(github.pages(anyString(), anyString())).thenReturn(List.of(measurement("in_progress")));
        when(github.get("/actions/runs/99/attempts/1")).thenReturn(mapper.valueToTree(Map.of(
                "head_branch", "main", "path", ".github/workflows/backend-prod.yml")));
        assertThatThrownBy(() -> service.link(SESSION, new MonitoringLinkRequest(99L, 1)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(github, never()).post(anyString(), any());
    }

    @Test
    @DisplayName("측정 결과가 아닌 파일은 다운로드하지 않는다")
    void rejectsUnrelatedArtifactAndDoesNotDownloadIt() {
        when(github.get("/actions/artifacts/99")).thenReturn(mapper.valueToTree(Map.of("name", "android-ci-reports")));
        assertThatThrownBy(() -> service.result(99)).isInstanceOf(IllegalArgumentException.class);
        verify(github, never()).artifact(99, "result.json");
    }

    @Test
    @DisplayName("실행이 대기 중이면 요청 시작과 관찰 종료를 추정하지 않는다")
    void queuedJobWithoutReadinessIsNotMeasuring() {
        when(github.pages("/actions/workflows/deployment-monitor.yml/runs?event=workflow_dispatch", "workflow_runs"))
                .thenReturn(List.of(measurement("queued")));
        when(github.pages("/actions/artifacts", "artifacts")).thenReturn(List.of());

        var response = service.session(SESSION);
        assertThat(response.state()).isNull();
        assertThat(response.run().status()).isEqualTo("queued");
        assertThat(response.resultAvailable()).isFalse();
        assertThat(response.observationEndsAt()).isNull();
    }

    @Test
    @DisplayName("배포 연결 완료 로그에는 측정·배포 식별자를 남기고 GitHub 응답과 토큰은 제외한다")
    void logsLinkIdentifiersWithoutCredentialsOrPayload() {
        when(github.pages(anyString(), anyString())).thenReturn(List.of(measurement("in_progress")));
        when(github.get("/actions/runs/99/attempts/2")).thenReturn(mapper.valueToTree(Map.of(
                "head_branch", "develop", "path", ".github/workflows/backend-dev.yml", "display_title", "private-title")));

        try (var logs = new LogCapture(DeploymentMonitoringService.class.getName())) {
            service.link(SESSION, new MonitoringLinkRequest(99L, 2));

            var event = logs.event("admin.monitoring.link", "success");
            assertThat(logs.field(event, "session")).isEqualTo(SESSION);
            assertThat(logs.field(event, "deploymentRunId")).isEqualTo(99L);
            assertThat(logs.field(event, "attempt")).isEqualTo(2);
            assertThat(logs.structuredText()).doesNotContain("test-token", "private-title");
        }
        verify(github).post("/actions/workflows/deployment-monitor.yml/dispatches", Map.of("ref", "develop",
                "inputs", Map.of("environment", "dev", "session", SESSION, "command", "link",
                        "measurement_run_id", "42", "deployment_run_id", "99", "deployment_attempt", "2")));
    }

    @Test
    @DisplayName("GitHub가 시작 요청을 거부하면 성공 로그를 남기지 않는다")
    void doesNotLogSuccessAfterFailedDispatch() {
        when(github.pages(anyString(), anyString())).thenReturn(List.of());
        org.mockito.Mockito.doThrow(new IllegalStateException("GitHub 연결 실패"))
                .when(github).post(anyString(), any());

        try (var logs = new LogCapture(DeploymentMonitoringService.class.getName())) {
            assertThatThrownBy(service::start).isInstanceOf(IllegalStateException.class);

            assertThat(logs.events("admin.monitoring.start")).isNotEmpty()
                    .allSatisfy(event -> assertThat(logs.hasField(event, "result")).isFalse());
        }
    }

    private JsonNode measurement(String status) {
        return mapper.valueToTree(Map.of("id", 42, "display_title", "monitor:dev:" + SESSION + ":start",
                "status", status, "path", ".github/workflows/deployment-monitor.yml"));
    }
}
