package yeobaek.backend.preregistration.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class PreRegistrationRateLimitInterceptorTest {

    @Test
    @DisplayName("Docker bridge 프록시 요청은 유효한 X-Real-IP를 클라이언트 IP로 사용한다")
    void useRealIpFromDockerBridgeProxy() {
        PreRegistrationRateLimiter rateLimiter = mock(PreRegistrationRateLimiter.class);
        var interceptor = new PreRegistrationRateLimitInterceptor(rateLimiter);
        MockHttpServletRequest request = postRequest(dockerBridgeIp());
        request.addHeader("X-Real-IP", testIpv4(30));

        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        verify(rateLimiter).check(testIpv4(30));
    }

    @Test
    @DisplayName("형식이 잘못된 X-Real-IP는 무시하고 remoteAddr를 사용한다")
    void fallBackToRemoteAddressForInvalidRealIp() {
        PreRegistrationRateLimiter rateLimiter = mock(PreRegistrationRateLimiter.class);
        var interceptor = new PreRegistrationRateLimitInterceptor(rateLimiter);
        MockHttpServletRequest request = postRequest(dockerBridgeIp());
        request.addHeader("X-Real-IP", "attacker-controlled-key");

        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        verify(rateLimiter).check(dockerBridgeIp());
    }

    @Test
    @DisplayName("길이 제한을 넘는 X-Real-IP는 remoteAddr로 대체한다")
    void fallBackToRemoteAddressForOverlongRealIp() {
        PreRegistrationRateLimiter rateLimiter = mock(PreRegistrationRateLimiter.class);
        var interceptor = new PreRegistrationRateLimitInterceptor(rateLimiter);
        MockHttpServletRequest request = postRequest(dockerBridgeIp());
        request.addHeader("X-Real-IP", "1".repeat(46));

        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        verify(rateLimiter).check(dockerBridgeIp());
    }

    @Test
    @DisplayName("IPv6 X-Real-IP도 구조가 유효하면 클라이언트 IP로 사용한다")
    void useValidIpv6RealIp() {
        PreRegistrationRateLimiter rateLimiter = mock(PreRegistrationRateLimiter.class);
        var interceptor = new PreRegistrationRateLimitInterceptor(rateLimiter);
        MockHttpServletRequest request = postRequest(dockerBridgeIp());
        request.addHeader("X-Real-IP", testIpv6());

        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        verify(rateLimiter).check(testIpv6());
    }

    @Test
    @DisplayName("서로 다른 X-Real-IP는 독립된 제한 키로 전달한다")
    void separateDifferentRealIps() {
        PreRegistrationRateLimiter rateLimiter = mock(PreRegistrationRateLimiter.class);
        var interceptor = new PreRegistrationRateLimitInterceptor(rateLimiter);
        MockHttpServletRequest firstRequest = postRequest(dockerBridgeIp());
        firstRequest.addHeader("X-Real-IP", testIpv4(30));
        MockHttpServletRequest secondRequest = postRequest(dockerBridgeIp());
        secondRequest.addHeader("X-Real-IP", testIpv4(31));

        interceptor.preHandle(firstRequest, new MockHttpServletResponse(), new Object());
        interceptor.preHandle(secondRequest, new MockHttpServletResponse(), new Object());

        verify(rateLimiter).check(testIpv4(30));
        verify(rateLimiter).check(testIpv4(31));
    }

    @Test
    @DisplayName("OPTIONS preflight는 요청 횟수에 포함하지 않는다")
    void ignoreOptionsPreflight() {
        PreRegistrationRateLimiter rateLimiter = mock(PreRegistrationRateLimiter.class);
        var interceptor = new PreRegistrationRateLimitInterceptor(rateLimiter);
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/pre-registrations");

        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        verifyNoInteractions(rateLimiter);
    }

    private MockHttpServletRequest postRequest(String remoteAddress) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/pre-registrations");
        request.setRemoteAddr(remoteAddress);
        return request;
    }

    private static String dockerBridgeIp() {
        return "172.18.0." + 1;
    }

    private static String testIpv4(int host) {
        return "198.51.100." + host;
    }

    private static String testIpv6() {
        return "2001:db8" + "::" + "8a2e:370:7334";
    }
}
