package yeobaek.backend.admin.controller;

import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.RESULT;
import static yeobaek.backend.support.LogField.SUCCESS;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import yeobaek.backend.admin.dto.MonitoringLinkRequest;
import yeobaek.backend.admin.dto.MonitoringOverviewResponse;
import yeobaek.backend.admin.dto.MonitoringRunResponse;
import yeobaek.backend.admin.dto.MonitoringSessionResponse;
import yeobaek.backend.admin.dto.MonitoringStartResponse;
import yeobaek.backend.admin.service.DeploymentMonitoringService;

@Tag(name = "관리자 배포 모니터링")
@Slf4j
@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "adminToken")
@RequestMapping("/api/admin/deployment-monitoring")
public class DeploymentMonitoringController {

    private final DeploymentMonitoringService service;

    @ModelAttribute
    public void preventCaching(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
    }

    @GetMapping
    @Operation(summary = "측정·결과 목록 조회",
            description = "현재 환경의 최근 측정 실행과 보관 중인 결과를 각각 최대 20개 반환한다.")
    public MonitoringOverviewResponse overview() {
        log.atInfo().addKeyValue(OPERATION, "admin.monitoring.api.overview")
                .log("측정 목록 API 처리를 시작합니다.");

        MonitoringOverviewResponse response = service.overview();

        log.atInfo().addKeyValue(OPERATION, "admin.monitoring.api.overview").addKeyValue(RESULT, SUCCESS)
                .addKeyValue("measurementCount", response.measurements().size())
                .addKeyValue("resultCount", response.results().size()).log("측정 목록 API 처리를 완료했습니다.");
        return response;
    }

    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "측정 시작 요청",
            description = "202는 시작 요청 접수이며 실제 측정 시작을 뜻하지 않는다. 진행 중인 측정이 있으면 기존 session을 반환한다.")
    public MonitoringStartResponse start() {
        log.atInfo().addKeyValue(OPERATION, "admin.monitoring.api.start")
                .log("측정 시작 요청 API 처리를 시작합니다.");

        MonitoringStartResponse response = service.start();

        log.atInfo().addKeyValue(OPERATION, "admin.monitoring.api.start").addKeyValue(RESULT, SUCCESS)
                .addKeyValue("session", response.session()).log("측정 시작 요청 API 처리를 완료했습니다.");
        return response;
    }

    @GetMapping("/sessions/{session}")
    @Operation(summary = "측정 진행 상황 조회",
            description = "state가 null이면 요청 시작이 아직 확인되지 않았다. run.status와 resultAvailable을 함께 확인한다. 10초 간격으로 조회한다.")
    public MonitoringSessionResponse session(@Parameter(description = "측정 시작 응답의 UUID") @PathVariable String session) {
        log.atInfo().addKeyValue(OPERATION, "admin.monitoring.api.session")
                .log("측정 상태 API 처리를 시작합니다.");

        MonitoringSessionResponse response = service.session(session);

        log.atInfo().addKeyValue(OPERATION, "admin.monitoring.api.session").addKeyValue(RESULT, SUCCESS)
                .addKeyValue("session", response.run().session())
                .addKeyValue("resultAvailable", response.resultAvailable()).log("측정 상태 API 처리를 완료했습니다.");
        return response;
    }

    @PostMapping("/sessions/{session}/stop")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "측정 종료 요청",
            description = "202 접수 후 진행 중인 요청과 결과 저장을 마무리한다. resultAvailable이 true가 될 때까지 상태를 조회한다.")
    public void stop(@Parameter(description = "측정 시작 응답의 UUID") @PathVariable String session) {
        log.atInfo().addKeyValue(OPERATION, "admin.monitoring.api.stop")
                .log("측정 종료 요청 API 처리를 시작합니다.");

        service.stop(session);

        log.atInfo().addKeyValue(OPERATION, "admin.monitoring.api.stop").addKeyValue(RESULT, SUCCESS)
                .addKeyValue("session", session).log("측정 종료 요청 API 처리를 완료했습니다.");
    }

    @PostMapping("/sessions/{session}/deployments")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "측정에 배포·롤백 연결",
            description = "배포 목록에서 선택한 id와 attempt를 보낸다. 202는 연결 요청 접수이며 반영 상태는 commands와 deployments에서 확인한다.")
    public void link(@Parameter(description = "측정 시작 응답의 UUID") @PathVariable String session,
                     @Valid @RequestBody MonitoringLinkRequest link) {
        log.atInfo().addKeyValue(OPERATION, "admin.monitoring.api.link")
                .log("배포 연결 요청 API 처리를 시작합니다.");

        service.link(session, link);

        log.atInfo().addKeyValue(OPERATION, "admin.monitoring.api.link").addKeyValue(RESULT, SUCCESS)
                .addKeyValue("session", session).addKeyValue("deploymentRunId", link.runId())
                .addKeyValue("attempt", link.attempt()).log("배포 연결 요청 API 처리를 완료했습니다.");
    }

    @GetMapping("/deployments")
    @Operation(summary = "연결 가능한 배포·롤백 목록 조회",
            description = "현재 환경에 허용된 워크플로의 최근 실행을 최대 20개 반환한다. id와 attempt를 함께 사용한다.")
    public List<MonitoringRunResponse> deployments() {
        log.atInfo().addKeyValue(OPERATION, "admin.monitoring.api.deployments")
                .log("배포 목록 API 처리를 시작합니다.");

        List<MonitoringRunResponse> response = service.deployments();

        log.atInfo().addKeyValue(OPERATION, "admin.monitoring.api.deployments").addKeyValue(RESULT, SUCCESS)
                .addKeyValue("resultCount", response.size()).log("배포 목록 API 처리를 완료했습니다.");
        return response;
    }

    @GetMapping("/results/{artifactId}")
    @Operation(summary = "저장된 측정 결과 조회",
            description = "목록의 결과 id(artifactId)로 result.json을 조회한다. 복구 미확인 구간의 기간은 null이며 중단 시간 합계에서 제외한다.")
    @ApiResponse(responseCode = "200", description = "측정 설정, summary, samples, deployments를 담은 결과 파일",
            content = @Content(mediaType = "application/json", schema = @Schema(type = "object", implementation = Map.class,
                    example = """
                            {"session":"11111111-1111-1111-1111-111111111111","environment":"dev",
                             "summary":{"total":2,"successes":1,"failures":1,"estimatedRecoveredSeconds":0,
                             "recoveryUnconfirmed":true,"outages":[{"startedAt":"2026-11-11T00:00:01Z",
                             "recoveredAt":null,"estimatedSeconds":null}],"versions":[]},
                             "samples":[{"sequence":0,"startedAt":"2026-11-11T00:00:00Z","statusCode":200,
                             "elapsedMs":12,"success":true,"error":null,"commitSha":null},
                             {"sequence":1,"startedAt":"2026-11-11T00:00:01Z","statusCode":502,
                             "elapsedMs":10,"success":false,"error":"http_error","commitSha":null}],
                             "deployments":[],"stopReason":"manual"}
                            """)))
    public JsonNode result(@Parameter(description = "보관 결과 목록의 id") @PathVariable long artifactId) {
        log.atInfo().addKeyValue(OPERATION, "admin.monitoring.api.result")
                .log("측정 결과 API 처리를 시작합니다.");

        JsonNode response = service.result(artifactId);

        log.atInfo().addKeyValue(OPERATION, "admin.monitoring.api.result").addKeyValue(RESULT, SUCCESS)
                .addKeyValue("artifactId", artifactId).log("측정 결과 API 처리를 완료했습니다.");
        return response;
    }
}
