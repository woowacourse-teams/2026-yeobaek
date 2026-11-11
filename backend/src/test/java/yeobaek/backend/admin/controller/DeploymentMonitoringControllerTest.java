package yeobaek.backend.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;
import yeobaek.backend.admin.dto.MonitoringLinkRequest;
import yeobaek.backend.admin.dto.MonitoringOverviewResponse;
import yeobaek.backend.admin.dto.MonitoringRunResponse;
import yeobaek.backend.admin.dto.MonitoringSessionResponse;
import yeobaek.backend.admin.dto.MonitoringStartResponse;
import yeobaek.backend.admin.service.DeploymentMonitoringService;
import yeobaek.backend.support.ControllerTest;

@WebMvcTest(DeploymentMonitoringController.class)
@TestPropertySource(properties = "admin.token=controller-test-token")
class DeploymentMonitoringControllerTest extends ControllerTest {

    private static final String BASE = "/api/admin/deployment-monitoring";
    private static final String SESSION = "11111111-1111-1111-1111-111111111111";
    @MockitoBean
    private DeploymentMonitoringService service;

    @Test
    void overviewReturnsEnvironmentAndEmptyCollectionsWithoutCaching() throws Exception {
        given(service.overview()).willReturn(new MonitoringOverviewResponse("dev", List.of(), List.of()));
        mockMvc.perform(get(BASE).header("X-Admin-Token", "controller-test-token"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.environment").value("dev"))
                .andExpect(jsonPath("$.measurements").isEmpty()).andExpect(jsonPath("$.results").isEmpty());
        verify(service, times(1)).overview();
    }

    @Test
    void startReturnsAcceptedSessionRatherThanClaimingReadiness() throws Exception {
        given(service.start()).willReturn(new MonitoringStartResponse(SESSION));
        mockMvc.perform(post(BASE + "/sessions").header("X-Admin-Token", "controller-test-token"))
                .andExpect(status().isAccepted()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.session").value(SESSION));
        verify(service, times(1)).start();
    }

    @Test
    void stopAcceptsCommandWithEmptyBody() throws Exception {
        mockMvc.perform(post(BASE + "/sessions/" + SESSION + "/stop").header("X-Admin-Token", "controller-test-token"))
                .andExpect(status().isAccepted()).andExpect(content().string(""));
        verify(service, times(1)).stop(SESSION);
    }

    @Test
    void linkBindsExactRunAndAttempt() throws Exception {
        mockMvc.perform(post(BASE + "/sessions/" + SESSION + "/deployments")
                        .header("X-Admin-Token", "controller-test-token").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"runId\":42,\"attempt\":2}"))
                .andExpect(status().isAccepted()).andExpect(content().string(""));
        verify(service, times(1)).link(SESSION, new MonitoringLinkRequest(42L, 2));
    }

    @Test
    void linkRejectsMissingAttemptBeforeService() throws Exception {
        mockMvc.perform(post(BASE + "/sessions/" + SESSION + "/deployments")
                        .header("X-Admin-Token", "controller-test-token").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"runId\":42}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void sessionReturnsAllFieldsWhileWaiting() throws Exception {
        var run = new MonitoringRunResponse(42L, 1, SESSION, "measurement", "queued", null,
                "abc", "2026-11-11T00:00:00Z", "https://github.com/test/runs/42", "queued");
        given(service.session(SESSION)).willReturn(new MonitoringSessionResponse(run, null, List.of(), List.of(),
                false, "2026-11-11T00:01:00Z", null));
        mockMvc.perform(get(BASE + "/sessions/" + SESSION).header("X-Admin-Token", "controller-test-token"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.run.id").value(42)).andExpect(jsonPath("$.run.attempt").value(1))
                .andExpect(jsonPath("$.run.session").value(SESSION)).andExpect(jsonPath("$.run.title").value("measurement"))
                .andExpect(jsonPath("$.run.status").value("queued")).andExpect(jsonPath("$.run.conclusion").isEmpty())
                .andExpect(jsonPath("$.run.sha").value("abc")).andExpect(jsonPath("$.run.createdAt").value("2026-11-11T00:00:00Z"))
                .andExpect(jsonPath("$.run.url").value("https://github.com/test/runs/42"))
                .andExpect(jsonPath("$.run.phase").value("queued"))
                .andExpect(jsonPath("$.state").isEmpty()).andExpect(jsonPath("$.deployments").isEmpty())
                .andExpect(jsonPath("$.commands").isEmpty()).andExpect(jsonPath("$.resultAvailable").value(false))
                .andExpect(jsonPath("$.checkedAt").value("2026-11-11T00:01:00Z"))
                .andExpect(jsonPath("$.observationEndsAt").isEmpty());
        verify(service, times(1)).session(SESSION);
    }

    @Test
    void deploymentListCanBeEmpty() throws Exception {
        given(service.deployments()).willReturn(List.of());
        mockMvc.perform(get(BASE + "/deployments").header("X-Admin-Token", "controller-test-token"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isEmpty());
        verify(service, times(1)).deployments();
    }

    @Test
    void resultReturnsStoredEvidence() throws Exception {
        given(service.result(123L)).willReturn(new ObjectMapper().readTree("{\"summary\":{\"failures\":2}}"));
        mockMvc.perform(get(BASE + "/results/123").header("X-Admin-Token", "controller-test-token"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.summary.failures").value(2));
        verify(service, times(1)).result(123L);
    }

    @Test
    void rejectsUnauthenticatedCommands() throws Exception {
        mockMvc.perform(post(BASE + "/sessions")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void propagatesServiceFailure() throws Exception {
        var failure = new IllegalStateException("GitHub 연결 실패");
        given(service.overview()).willThrow(failure);
        var result = mockMvc.perform(get(BASE).header("X-Admin-Token", "controller-test-token"))
                .andExpect(status().isInternalServerError()).andReturn();
        assertThat(result.getResolvedException()).isSameAs(failure);
        verify(service, times(1)).overview();
    }
}
