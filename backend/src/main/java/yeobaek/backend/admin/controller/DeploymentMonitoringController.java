package yeobaek.backend.admin.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
    public MonitoringOverviewResponse overview() {
        return service.overview();
    }

    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MonitoringStartResponse start() {
        return service.start();
    }

    @GetMapping("/sessions/{session}")
    public MonitoringSessionResponse session(@PathVariable String session) {
        return service.session(session);
    }

    @PostMapping("/sessions/{session}/stop")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void stop(@PathVariable String session) {
        service.stop(session);
    }

    @PostMapping("/sessions/{session}/deployments")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void link(@PathVariable String session, @Valid @RequestBody MonitoringLinkRequest link) {
        service.link(session, link);
    }

    @GetMapping("/deployments")
    public List<MonitoringRunResponse> deployments() {
        return service.deployments();
    }

    @GetMapping("/results/{artifactId}")
    public JsonNode result(@PathVariable long artifactId) {
        return service.result(artifactId);
    }
}
