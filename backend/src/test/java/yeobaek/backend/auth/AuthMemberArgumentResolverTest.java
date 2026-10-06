package yeobaek.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import yeobaek.backend.shared.identity.MemberId;

class AuthMemberArgumentResolverTest {

    private final AuthMemberArgumentResolver resolver = new AuthMemberArgumentResolver();

    @Test
    void supportsLegacyLongAndCanonicalMemberId() throws Exception {
        assertThat(resolver.supportsParameter(parameter("legacy", Long.class))).isTrue();
        assertThat(resolver.supportsParameter(parameter("canonical", MemberId.class))).isTrue();
        assertThat(resolver.supportsParameter(parameter("unsupported", String.class))).isFalse();
        assertThat(resolver.supportsParameter(parameter("unannotated", MemberId.class))).isFalse();
    }

    @Test
    void resolveLegacyLongWithoutConversion() throws Exception {
        NativeWebRequest request = requestWithMemberId(7L);

        Object resolved = resolver.resolveArgument(parameter("legacy", Long.class), null, request, null);

        assertThat(resolved).isEqualTo(7L);
    }

    @Test
    void resolveCanonicalMemberId() throws Exception {
        NativeWebRequest request = requestWithMemberId(7L);

        Object resolved = resolver.resolveArgument(parameter("canonical", MemberId.class), null, request, null);

        assertThat(resolved).isEqualTo(new MemberId(7L));
    }

    private NativeWebRequest requestWithMemberId(long memberId) {
        NativeWebRequest request = mock(NativeWebRequest.class);
        given(request.getAttribute(MemberAuthInterceptor.MEMBER_ID_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST))
                .willReturn(memberId);
        return request;
    }

    private MethodParameter parameter(String methodName, Class<?> parameterType) throws NoSuchMethodException {
        return new MethodParameter(Handler.class.getDeclaredMethod(methodName, parameterType), 0);
    }

    abstract static class Handler {

        abstract void legacy(@AuthMember Long memberId);

        abstract void canonical(@AuthMember MemberId memberId);

        abstract void unsupported(@AuthMember String memberId);

        abstract void unannotated(MemberId memberId);
    }
}
