package yeobaek.backend.preregistration.controller;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import yeobaek.backend.preregistration.service.PreRegistrationService;
import yeobaek.backend.support.ConflictException;
import yeobaek.backend.support.ControllerTest;
import yeobaek.backend.support.ErrorCode;

@WebMvcTest(PreRegistrationController.class)
class PreRegistrationControllerTest extends ControllerTest {

    private static final String LANDING_ORIGIN = "https://yeobaek-landing-hypothesis.vercel.app";

    @MockitoBean
    private PreRegistrationService preRegistrationService;

    @Test
    @DisplayName("회원 헤더 없이 사전신청 요청을 서비스에 전달하고 201을 반환한다")
    void createPreRegistrationWithoutMemberHeader() throws Exception {
        mockMvc.perform(post("/api/pre-registrations")
                        .with(remoteAddress(testIpv4(10)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"Reader@Example.com"}
                                """))
                .andExpect(status().isCreated());

        verify(preRegistrationService, times(1)).create("Reader@Example.com");
    }

    @Test
    @DisplayName("요청 본문이 없으면 서비스를 호출하지 않는다")
    void rejectMissingBody() throws Exception {
        mockMvc.perform(post("/api/pre-registrations")
                        .with(remoteAddress(testIpv4(11)))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> assertInstanceOf(
                        HttpMessageNotReadableException.class,
                        result.getResolvedException()));

        verifyNoInteractions(preRegistrationService);
    }

    @Test
    @DisplayName("서비스 예외를 변경하지 않고 전파한다")
    void propagateServiceException() throws Exception {
        var serviceException = new ConflictException(ErrorCode.PRE_REGISTRATION_ALREADY_EXISTS);
        willThrow(serviceException).given(preRegistrationService).create("reader@example.com");

        var result = mockMvc.perform(post("/api/pre-registrations")
                        .with(remoteAddress(testIpv4(12)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"reader@example.com"}
                                """))
                .andReturn();

        assertSame(serviceException, result.getResolvedException(),
                "컨트롤러는 서비스 예외 인스턴스를 변경하지 않아야 한다");
        verify(preRegistrationService, times(1)).create("reader@example.com");
    }

    @Test
    @DisplayName("운영 랜딩 출처의 POST preflight를 허용한다")
    void allowConfiguredLandingOrigin() throws Exception {
        mockMvc.perform(options("/api/pre-registrations")
                        .header("Origin", LANDING_ORIGIN)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", LANDING_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Methods", "POST"));
    }

    @Test
    @DisplayName("설정하지 않은 출처의 preflight는 허용하지 않는다")
    void rejectUnconfiguredOrigin() throws Exception {
        mockMvc.perform(options("/api/pre-registrations")
                        .header("Origin", "https://example.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("같은 IP의 다섯 요청은 허용하고 여섯 번째 요청은 429로 차단한다")
    void rateLimitByClientIp() throws Exception {
        for (int requestCount = 0; requestCount < 5; requestCount++) {
            mockMvc.perform(post("/api/pre-registrations")
                            .with(remoteAddress(testIpv4(20)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"reader@example.com"}
                                    """))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(post("/api/pre-registrations")
                        .with(remoteAddress(testIpv4(20)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"reader@example.com"}
                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.message").value("요청이 너무 많습니다. 잠시 후 다시 시도해 주세요."));

        verify(preRegistrationService, times(5)).create("reader@example.com");
    }

    private static RequestPostProcessor remoteAddress(String address) {
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }

    private static String testIpv4(int host) {
        return "198.51.100." + host;
    }
}
