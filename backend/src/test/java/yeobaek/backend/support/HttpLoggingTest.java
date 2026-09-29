package yeobaek.backend.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import yeobaek.backend.member.controller.MemberController;
import yeobaek.backend.member.service.MemberBlockService;
import yeobaek.backend.member.service.MemberService;
import yeobaek.backend.support.analytics.AnalyticsTracker;

@WebMvcTest(MemberController.class)
class HttpLoggingTest extends ControllerTest {

    @MockitoBean
    private MemberService memberService;
    @MockitoBean
    private MemberBlockService memberBlockService;
    @MockitoBean
    private AnalyticsTracker analyticsTracker;

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void correlateAuthenticatedRequestAndClearContext() throws Exception {
        givenValidMember(7L);
        try (var logs = new LogCapture("yeobaek.backend")) {
            mockMvc.perform(delete("/api/members/me").header("X-Member-Id", "7"))
                    .andExpect(status().isNoContent());

            ILoggingEvent completion = logs.events().getLast();
            String traceId = completion.getMDCPropertyMap().get("traceId");
            assertThat(logs.events()).allSatisfy(event ->
                    assertThat(event.getMDCPropertyMap()).containsEntry("traceId", traceId));
            assertThat(traceId).isNotBlank();
            assertThat(completion.getMDCPropertyMap()).containsEntry("memberId", "7");
            assertThat(logs.field(completion, "status")).isEqualTo(204);
            assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
        }
    }

    @Test
    void authenticationRejectionIsInfoAndDoesNotExposeInput() throws Exception {
        MDC.put("memberId", "stale-member");
        try (var logs = new LogCapture("yeobaek.backend")) {
            mockMvc.perform(get("/api/members/me/blocks")
                            .header("X-Member-Id", "secret-header")
                            .header("X-Admin-Token", "secret-admin")
                            .queryParam("token", "secret-query"))
                    .andExpect(status().isBadRequest());

            assertThat(logs.events()).allSatisfy(event -> assertThat(event.getLevel()).isEqualTo(Level.INFO));
            assertThat(logs.structuredText()).doesNotContain("secret-header", "secret-admin", "secret-query", "stale-member");
            assertThat(logs.field(logs.events().getLast(), "status")).isEqualTo(400);
            assertThat(MDC.get("memberId")).isEqualTo("stale-member");
            verifyNoInteractions(memberService, memberBlockService, analyticsTracker);
        }
    }

    @Test
    void serverFailureHasOneStackAndNoControllerSuccess() throws Exception {
        givenValidMember(7L);
        doThrow(new IllegalStateException("server failure")).when(memberService).delete(7L);
        try (var logs = new LogCapture("yeobaek.backend")) {
            mockMvc.perform(delete("/api/members/me").header("X-Member-Id", "7"))
                    .andExpect(status().isInternalServerError());

            assertThat(logs.events().stream().filter(event -> event.getThrowableProxy() != null)).hasSize(1);
            assertThat(logs.events().getLast().getLevel()).isEqualTo(Level.ERROR);
            assertThat(logs.field(logs.events().getLast(), "status")).isEqualTo(500);
            assertThat(logs.events().stream().filter(event -> event.getLoggerName().equals(MemberController.class.getName())))
                    .allSatisfy(event -> assertThat(logs.field(event, "phase")).isEqualTo("attempt"));
            assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
        }
    }

    @Test
    void unhandledMvcFailureIsLoggedAndPropagated() {
        givenValidMember(7L);
        doThrow(new NullPointerException("unexpected failure")).when(memberService).delete(7L);
        try (var logs = new LogCapture("yeobaek.backend")) {
            assertThatThrownBy(() -> mockMvc.perform(delete("/api/members/me").header("X-Member-Id", "7")))
                    .hasRootCauseInstanceOf(NullPointerException.class);
            assertThat(logs.events().getLast().getLevel()).isEqualTo(Level.ERROR);
            assertThat(logs.field(logs.events().getLast(), "status")).isEqualTo(500);
            assertThat(logs.events().getLast().getThrowableProxy()).isNotNull();
            assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
        }
    }
}
