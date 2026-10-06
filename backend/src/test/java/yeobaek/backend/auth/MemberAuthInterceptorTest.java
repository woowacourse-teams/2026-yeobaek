package yeobaek.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.support.LogCapture;
import yeobaek.backend.shared.exception.NotFoundException;

class MemberAuthInterceptorTest {

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final MemberAuthInterceptor interceptor = new MemberAuthInterceptor(memberRepository);

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("인증된 요청자는 MDC에 한 번 설정하고 성공 로그에 같은 키를 중복하지 않는다")
    void putAuthenticatedMemberInMdc() {
        given(memberRepository.existsById(7L)).willReturn(true);
        var request = new MockHttpServletRequest();
        request.addHeader("X-Member-Id", "7");

        try (var logs = new LogCapture(MemberAuthInterceptor.class.getName())) {
            assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();

            var event = logs.event("auth.member.preHandle", "success");
            assertThat(MDC.get("memberId")).isEqualTo("7");
            assertThat(event.getMDCPropertyMap()).containsEntry("memberId", "7");
            assertThat(logs.hasField(event, "memberId")).isFalse();
        }
    }

    @Test
    @DisplayName("존재하지 않는 헤더 회원은 인증 전 값임을 드러내고 MDC에 넣지 않는다")
    void rejectUnknownRequestedMember() {
        var request = new MockHttpServletRequest();
        request.addHeader("X-Member-Id", "7");

        assertThatThrownBy(() -> interceptor.preHandle(
                request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(NotFoundException.class)
                .satisfies(exception -> assertThat(((NotFoundException) exception).getLogContext())
                        .containsEntry("requestedMemberId", "7")
                        .doesNotContainKey("memberId"));
        assertThat(MDC.get("memberId")).isNull();
    }
}
