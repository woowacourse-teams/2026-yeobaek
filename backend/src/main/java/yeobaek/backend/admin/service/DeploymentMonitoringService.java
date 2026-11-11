package yeobaek.backend.admin.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import yeobaek.backend.admin.dto.MonitoringArtifactResponse;
import yeobaek.backend.admin.dto.MonitoringCommandResponse;
import yeobaek.backend.admin.dto.MonitoringLinkRequest;
import yeobaek.backend.admin.dto.MonitoringOverviewResponse;
import yeobaek.backend.admin.dto.MonitoringRunResponse;
import yeobaek.backend.admin.dto.MonitoringSessionResponse;
import yeobaek.backend.admin.dto.MonitoringStartResponse;
import yeobaek.backend.support.monitoring.MonitoringGitHubClient;
import yeobaek.backend.support.monitoring.MonitoringSettings;

@Service
@RequiredArgsConstructor
public class DeploymentMonitoringService {

    private static final String WORKFLOW = "deployment-monitor.yml";
    private static final String LINK_COMMAND = "link";
    private final MonitoringGitHubClient github;
    private final MonitoringSettings settings;

    public MonitoringOverviewResponse overview() {
        List<MonitoringRunResponse> measurements = measurementRuns().stream().limit(20).map(this::toRun).toList();
        String prefix = "ym-result-" + settings.getEnvironment() + "-";
        List<MonitoringArtifactResponse> results = artifacts().stream()
                .filter(node -> text(node, "name").startsWith(prefix))
                .sorted(Comparator.comparingLong((JsonNode node) -> node.path("id").asLong()).reversed())
                .limit(20)
                .map(node -> new MonitoringArtifactResponse(node.path("id").asLong(),
                        text(node, "name").substring(prefix.length()), text(node, "created_at"),
                        text(node, "expires_at"), node.path("size_in_bytes").asLong())).toList();
        return new MonitoringOverviewResponse(settings.getEnvironment(), measurements, results);
    }

    public MonitoringStartResponse start() {
        settings.requireConfigured();
        List<JsonNode> active = measurementRuns().stream().filter(this::active).toList();
        if (!active.isEmpty()) {
            return new MonitoringStartResponse(sessionOf(active.getFirst()));
        }
        String session = UUID.randomUUID().toString();
        dispatch(session, "start", Map.of());
        return new MonitoringStartResponse(session);
    }

    public void stop(String session) {
        JsonNode measurement = requireActive(session);
        dispatch(session, "stop", Map.of("measurement_run_id", text(measurement, "id")));
    }

    public void link(String session, MonitoringLinkRequest link) {
        JsonNode measurement = requireActive(session);
        JsonNode deployment = github.get("/actions/runs/" + link.runId() + "/attempts/" + link.attempt());
        validateDeployment(deployment);
        dispatch(session, "link", Map.of("measurement_run_id", text(measurement, "id"),
                "deployment_run_id", link.runId().toString(), "deployment_attempt", link.attempt().toString()));
    }

    public List<MonitoringRunResponse> deployments() {
        List<JsonNode> runs = new ArrayList<>();
        for (String workflow : settings.getDeploymentWorkflows()) {
            JsonNode response = github.get("/actions/workflows/" + workflow + "/runs?per_page=20&branch="
                    + settings.getDeploymentBranch());
            response.path("workflow_runs").forEach(runs::add);
        }
        return runs.stream().sorted(Comparator.comparingLong((JsonNode node) -> node.path("id").asLong()).reversed())
                .limit(20).map(this::toRun).toList();
    }

    public MonitoringSessionResponse session(String session) {
        JsonNode measurement = measurement(session);
        List<JsonNode> artifacts = artifacts();
        JsonNode stateArtifact = findArtifact(artifacts, "ym-state-" + settings.getEnvironment() + "-" + session);
        JsonNode state = stateArtifact == null ? null : readArtifact(stateArtifact, session, "state.json", "start");
        boolean resultAvailable = findArtifact(artifacts, "ym-result-" + settings.getEnvironment() + "-" + session) != null;
        List<MonitoringCommandResponse> commands = commandRuns(session);
        Map<String, JsonNode> linked = new LinkedHashMap<>();
        String commandPrefix = "ym-command-" + settings.getEnvironment() + "-" + session + "-";
        for (JsonNode artifact : artifacts) {
            if (text(artifact, "name").startsWith(commandPrefix)) {
                JsonNode command = readArtifact(artifact, session, "command.json", null);
                if (LINK_COMMAND.equals(text(command, "command"))) {
                    String path = "/actions/runs/" + command.path("deploymentRunId").asLong()
                            + "/attempts/" + command.path("attempt").asInt();
                    linked.put(path, command);
                }
            }
        }
        List<MonitoringRunResponse> deployments = new ArrayList<>();
        List<Instant> endings = new ArrayList<>();
        for (String path : linked.keySet()) {
            JsonNode run = github.get(path);
            validateDeployment(run);
            List<JsonNode> jobs = github.pages(path + "/jobs", "jobs");
            deployments.add(toRun(run, phase(run, jobs)));
            if (!active(run)) {
                endings.add(jobs.stream().filter(job -> !job.path("completed_at").isNull())
                        .map(job -> text(job, "completed_at")).filter(value -> !value.isBlank())
                        .map(Instant::parse).max(Comparator.naturalOrder())
                        .orElseGet(() -> Instant.parse(text(run, "updated_at"))));
            }
        }
        String observationEndsAt = !endings.isEmpty() && endings.size() == deployments.size()
                ? endings.stream().max(Comparator.naturalOrder()).orElseThrow().plusSeconds(300).toString() : null;
        return new MonitoringSessionResponse(toRun(measurement), state, deployments, commands,
                resultAvailable, Instant.now().toString(), observationEndsAt);
    }

    public JsonNode result(long artifactId) {
        JsonNode artifact = github.get("/actions/artifacts/" + artifactId);
        String prefix = "ym-result-" + settings.getEnvironment() + "-";
        String name = text(artifact, "name");
        if (!name.startsWith(prefix) || artifact.path("expired").asBoolean()) {
            throw new IllegalArgumentException("이 환경의 보관 중인 측정 결과가 아닙니다.");
        }
        String session = name.substring(prefix.length());
        validateSession(session);
        return readArtifact(artifact, session, "result.json", "start");
    }

    private List<JsonNode> measurementRuns() {
        String prefix = "monitor:" + settings.getEnvironment() + ":";
        return github.pages("/actions/workflows/" + WORKFLOW + "/runs?event=workflow_dispatch", "workflow_runs")
                .stream().filter(run -> text(run, "display_title").startsWith(prefix)
                        && text(run, "display_title").endsWith(":start"))
                .sorted(Comparator.comparingLong((JsonNode node) -> node.path("id").asLong()).reversed()).toList();
    }

    private List<MonitoringCommandResponse> commandRuns(String session) {
        String prefix = "monitor:" + settings.getEnvironment() + ":" + session + ":";
        return github.pages("/actions/workflows/" + WORKFLOW + "/runs?event=workflow_dispatch", "workflow_runs")
                .stream().filter(run -> text(run, "display_title").startsWith(prefix)
                        && !text(run, "display_title").endsWith(":start"))
                .map(run -> new MonitoringCommandResponse(text(run, "display_title").substring(prefix.length()),
                        text(run, "status"), nullable(run, "conclusion"), text(run, "html_url"))).toList();
    }

    private List<JsonNode> artifacts() {
        return github.pages("/actions/artifacts", "artifacts").stream()
                .filter(node -> !node.path("expired").asBoolean()).toList();
    }

    private JsonNode findArtifact(List<JsonNode> artifacts, String name) {
        return artifacts.stream().filter(node -> name.equals(text(node, "name"))).findFirst().orElse(null);
    }

    private JsonNode readArtifact(JsonNode artifact, String session, String filename, String command) {
        long sourceId = artifact.path("workflow_run").path("id").asLong();
        JsonNode source = github.get("/actions/runs/" + sourceId);
        String prefix = "monitor:" + settings.getEnvironment() + ":" + session + ":";
        String title = text(source, "display_title");
        if (!(".github/workflows/" + WORKFLOW).equals(text(source, "path").split("@")[0])
                || !title.startsWith(prefix) || (command != null && !title.equals(prefix + command))) {
            throw new IllegalArgumentException("측정 기록의 출처가 올바르지 않습니다.");
        }
        return github.artifact(artifact.path("id").asLong(), filename);
    }

    private JsonNode measurement(String session) {
        validateSession(session);
        return measurementRuns().stream().filter(run -> sessionOf(run).equals(session)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("측정 실행이 아직 목록에 없습니다. 잠시 후 다시 조회하세요."));
    }

    private JsonNode requireActive(String session) {
        JsonNode run = measurement(session);
        if (!active(run)) {
            throw new IllegalArgumentException("이미 종료된 측정입니다.");
        }
        return run;
    }

    private void validateSession(String session) {
        if (!session.matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}")) {
            throw new IllegalArgumentException("측정 식별자가 올바르지 않습니다.");
        }
    }

    private void validateDeployment(JsonNode run) {
        String path = text(run, "path").split("@")[0];
        if (!settings.getDeploymentBranch().equals(text(run, "head_branch"))
                || settings.getDeploymentWorkflows().stream().noneMatch(name -> path.equals(".github/workflows/" + name))) {
            throw new IllegalArgumentException("현재 환경의 배포·롤백 실행만 연결할 수 있습니다.");
        }
    }

    private void dispatch(String session, String command, Map<String, String> extra) {
        Map<String, String> inputs = new LinkedHashMap<>(extra);
        inputs.put("environment", settings.getEnvironment());
        inputs.put("session", session);
        inputs.put("command", command);
        github.post("/actions/workflows/" + WORKFLOW + "/dispatches", Map.of("ref", settings.getRef(), "inputs", inputs));
    }

    private MonitoringRunResponse toRun(JsonNode run) {
        return toRun(run, text(run, "status"));
    }

    private MonitoringRunResponse toRun(JsonNode run, String phase) {
        String session = text(run, "display_title").startsWith("monitor:") ? sessionOf(run) : null;
        return new MonitoringRunResponse(run.path("id").asLong(), run.path("run_attempt").asInt(1), session,
                text(run, "display_title"), text(run, "status"), nullable(run, "conclusion"),
                text(run, "head_sha"), text(run, "created_at"), text(run, "html_url"), phase);
    }

    private String phase(JsonNode run, List<JsonNode> jobs) {
        if (!active(run)) {
            return "completed";
        }
        if (jobs.stream().anyMatch(job -> "deploy".equals(text(job, "name"))
                && "in_progress".equals(text(job, "status")))) {
            return "deploying";
        }
        if (jobs.stream().anyMatch(job -> "build-and-push".equals(text(job, "name"))
                && "in_progress".equals(text(job, "status")))) {
            return "building";
        }
        return "in_progress".equals(text(run, "status")) ? "running" : "waiting";
    }

    private String sessionOf(JsonNode run) {
        return text(run, "display_title").split(":")[2];
    }

    private boolean active(JsonNode run) {
        return !"completed".equals(text(run, "status"));
    }

    private String text(JsonNode node, String field) {
        return node.path(field).asString("");
    }

    private String nullable(JsonNode node, String field) {
        return node.path(field).isNull() || node.path(field).isMissingNode() ? null : text(node, field);
    }
}
