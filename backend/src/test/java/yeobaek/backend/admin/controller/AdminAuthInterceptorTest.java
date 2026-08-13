package yeobaek.backend.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import yeobaek.backend.support.UnauthorizedException;

class AdminAuthInterceptorTest {

    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @Test
    @DisplayName("설정된 토큰과 헤더가 일치하면 통과한다")
    void passWithMatchingToken() {
        AdminAuthInterceptor interceptor = new AdminAuthInterceptor("secret");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Admin-Token", "secret");

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
    }

    @Test
    @DisplayName("헤더가 없거나 값이 다르면 401 예외를 던진다")
    void rejectMissingOrWrongToken() {
        AdminAuthInterceptor interceptor = new AdminAuthInterceptor("secret");
        MockHttpServletRequest wrong = new MockHttpServletRequest();
        wrong.addHeader("X-Admin-Token", "other");

        assertThatThrownBy(() -> interceptor.preHandle(new MockHttpServletRequest(), response, new Object()))
                .isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> interceptor.preHandle(wrong, response, new Object()))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("서버에 토큰이 설정되지 않으면 어떤 요청도 통과하지 못한다")
    void rejectAllWhenTokenUnconfigured() {
        AdminAuthInterceptor interceptor = new AdminAuthInterceptor("");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Admin-Token", "");

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(UnauthorizedException.class);
    }
}
