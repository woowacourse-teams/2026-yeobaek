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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;
import yeobaek.backend.admin.dto.MonitoringArtifactResponse;
import yeobaek.backend.admin.dto.MonitoringCommandResponse;
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
    @DisplayName("환경과 보관 결과 정보를 캐시 없이 반환한다")
    void overviewReturnsEnvironmentAndResultMetadataWithoutCaching() throws Exception {
        given(service.overview()).willReturn(new MonitoringOverviewResponse("dev", List.of(), List.of(
                new MonitoringArtifactResponse(123L, SESSION, "2026-11-11T00:00:00Z", "2027-02-09T00:00:00Z", 2048))));

        mockMvc.perform(get(BASE).header("X-Admin-Token", "controller-test-token"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.environment").value("dev"))
                .andExpect(jsonPath("$.measurements").isArray()).andExpect(jsonPath("$.measurements").isEmpty())
                .andExpect(jsonPath("$.results.length()").value(1))
                .andExpect(jsonPath("$.results[0].id").value(123))
                .andExpect(jsonPath("$.results[0].session").value(SESSION))
                .andExpect(jsonPath("$.results[0].createdAt").value("2026-11-11T00:00:00Z"))
                .andExpect(jsonPath("$.results[0].expiresAt").value("2027-02-09T00:00:00Z"))
                .andExpect(jsonPath("$.results[0].bytes").value(2048));

        verify(service, times(1)).overview();
    }

    @Test
    @DisplayName("시작 요청을 접수하고 측정 UUID를 반환한다")
    void startReturnsAcceptedSessionRatherThanClaimingReadiness() throws Exception {
        given(service.start()).willReturn(new MonitoringStartResponse(SESSION));

        mockMvc.perform(post(BASE + "/sessions").header("X-Admin-Token", "controller-test-token"))
                .andExpect(status().isAccepted()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.session").value(SESSION));

        verify(service, times(1)).start();
    }

    @Test
    @DisplayName("종료 요청을 접수하고 빈 본문을 반환한다")
    void stopAcceptsCommandWithEmptyBody() throws Exception {

        mockMvc.perform(post(BASE + "/sessions/" + SESSION + "/stop").header("X-Admin-Token", "controller-test-token"))
                .andExpect(status().isAccepted()).andExpect(content().string(""));

        verify(service, times(1)).stop(SESSION);
    }

    @Test
    @DisplayName("배포 실행 ID와 시도 번호를 서비스에 전달한다")
    void linkBindsExactRunAndAttempt() throws Exception {

        mockMvc.perform(post(BASE + "/sessions/" + SESSION + "/deployments")
                        .header("X-Admin-Token", "controller-test-token").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"runId\":42,\"attempt\":2}"))
                .andExpect(status().isAccepted()).andExpect(content().string(""));

        verify(service, times(1)).link(SESSION, new MonitoringLinkRequest(42L, 2));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"runId\":42}", "{\"attempt\":1}",
            "{\"runId\":0,\"attempt\":1}", "{\"runId\":42,\"attempt\":0}"})
    @DisplayName("필수 실행 ID와 시도 번호가 없거나 양수가 아니면 서비스를 호출하지 않는다")
    void linkRejectsInvalidRunOrAttemptBeforeService(String body) throws Exception {

        mockMvc.perform(post(BASE + "/sessions/" + SESSION + "/deployments")
                        .header("X-Admin-Token", "controller-test-token").contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("시작 대기 상태는 null과 빈 배열을 구분해 반환한다")
    void sessionReturnsAllFieldsWhileWaiting() throws Exception {
        var run = new MonitoringRunResponse(42L, 1, SESSION, "measurement", "queued", null,
                "abc", "2026-11-11T00:00:00Z", "https://github.com/test/runs/42", "queued");
        given(service.session(SESSION)).willReturn(new MonitoringSessionResponse(run, null, List.of(), List.of(),
                false, "2026-11-11T00:01:00Z", null));

        mockMvc.perform(get(BASE + "/sessions/" + SESSION).header("X-Admin-Token", "controller-test-token"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.run.id").value(42)).andExpect(jsonPath("$.run.attempt").value(1))
                .andExpect(jsonPath("$.run.session").value(SESSION)).andExpect(jsonPath("$.run.title").value("measurement"))
                .andExpect(jsonPath("$.run.status").value("queued")).andExpect(jsonPath("$.run.conclusion").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.run.sha").value("abc")).andExpect(jsonPath("$.run.createdAt").value("2026-11-11T00:00:00Z"))
                .andExpect(jsonPath("$.run.url").value("https://github.com/test/runs/42"))
                .andExpect(jsonPath("$.run.phase").value("queued"))
                .andExpect(jsonPath("$.state").value(org.hamcrest.Matchers.nullValue())).andExpect(jsonPath("$.deployments").isEmpty())
                .andExpect(jsonPath("$.commands").isEmpty()).andExpect(jsonPath("$.resultAvailable").value(false))
                .andExpect(jsonPath("$.checkedAt").value("2026-11-11T00:01:00Z"))
                .andExpect(jsonPath("$.observationEndsAt").value(org.hamcrest.Matchers.nullValue()));

        verify(service, times(1)).session(SESSION);
    }

    @Test
    @DisplayName("시작 확인 기록과 연결된 배포·명령 및 관찰 종료 시각을 전달한다")
    void sessionReturnsReadinessAndLinkedCommands() throws Exception {
        var run = new MonitoringRunResponse(42L, 1, SESSION, "measurement", "in_progress", null,
                "abc", "2026-11-11T00:00:00Z", "https://github.com/test/runs/42", "in_progress");
        var state = new ObjectMapper().readTree("{\"startedAt\":\"2026-11-11T00:00:01Z\"}");
        var deployment = new MonitoringRunResponse(99L, 2, null, "deploy", "completed", "success",
                "def", "2026-11-11T00:00:00Z", "https://github.com/test/runs/99", "completed");
        var command = new MonitoringCommandResponse("link", "completed", "success", "https://github.com/test/runs/100");
        given(service.session(SESSION)).willReturn(new MonitoringSessionResponse(run, state, List.of(deployment),
                List.of(command), true, "2026-11-11T00:02:00Z", "2026-11-11T00:06:00Z"));

        mockMvc.perform(get(BASE + "/sessions/" + SESSION).header("X-Admin-Token", "controller-test-token"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.state.startedAt").value("2026-11-11T00:00:01Z"))
                .andExpect(jsonPath("$.deployments.length()").value(1))
                .andExpect(jsonPath("$.deployments[0].id").value(99))
                .andExpect(jsonPath("$.deployments[0].attempt").value(2))
                .andExpect(jsonPath("$.deployments[0].session").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.deployments[0].title").value("deploy"))
                .andExpect(jsonPath("$.deployments[0].status").value("completed"))
                .andExpect(jsonPath("$.deployments[0].conclusion").value("success"))
                .andExpect(jsonPath("$.deployments[0].sha").value("def"))
                .andExpect(jsonPath("$.deployments[0].createdAt").value("2026-11-11T00:00:00Z"))
                .andExpect(jsonPath("$.deployments[0].url").value("https://github.com/test/runs/99"))
                .andExpect(jsonPath("$.deployments[0].phase").value("completed"))
                .andExpect(jsonPath("$.commands.length()").value(1))
                .andExpect(jsonPath("$.commands[0].command").value("link"))
                .andExpect(jsonPath("$.commands[0].status").value("completed"))
                .andExpect(jsonPath("$.commands[0].conclusion").value("success"))
                .andExpect(jsonPath("$.commands[0].url").value("https://github.com/test/runs/100"))
                .andExpect(jsonPath("$.resultAvailable").value(true))
                .andExpect(jsonPath("$.observationEndsAt").value("2026-11-11T00:06:00Z"));

        verify(service, times(1)).session(SESSION);
    }

    @Test
    @DisplayName("연결 가능한 배포가 없으면 빈 배열을 반환한다")
    void deploymentListCanBeEmpty() throws Exception {
        given(service.deployments()).willReturn(List.of());

        mockMvc.perform(get(BASE + "/deployments").header("X-Admin-Token", "controller-test-token"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isEmpty());

        verify(service, times(1)).deployments();
    }

    @Test
    @DisplayName("저장된 측정 결과 JSON을 전달한다")
    void resultReturnsStoredEvidence() throws Exception {
        given(service.result(123L)).willReturn(new ObjectMapper().readTree("{\"summary\":{\"failures\":2}}"));

        mockMvc.perform(get(BASE + "/results/123").header("X-Admin-Token", "controller-test-token"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.summary.failures").value(2));

        verify(service, times(1)).result(123L);
    }

    @Test
    @DisplayName("서비스 예외를 변경하지 않고 공통 예외 처리로 전달한다")
    void propagatesServiceFailure() throws Exception {
        var failure = new IllegalStateException("GitHub 연결 실패");
        given(service.overview()).willThrow(failure);
        var result = mockMvc.perform(get(BASE).header("X-Admin-Token", "controller-test-token"))
                .andExpect(status().isInternalServerError()).andReturn();
        assertThat(result.getResolvedException()).isSameAs(failure);

        verify(service, times(1)).overview();
    }
}
