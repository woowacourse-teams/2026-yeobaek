package yeobaek.backend.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExceptionLogContextTest {

    @Test
    @DisplayName("BadRequestException 기존 생성자는 에러 코드와 메시지를 유지하고 빈 로그 컨텍스트를 제공한다")
    void badRequestLegacyConstructor() {
        var badRequest = new BadRequestException(ErrorCode.INVALID_REQUEST, "잘못된 요청");

        assertThat(badRequest.getCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
        assertThat(badRequest.getMessage()).isEqualTo("잘못된 요청");
        assertThat(badRequest.getLogContext()).isEmpty();
    }

    @Test
    @DisplayName("NotFoundException 기존 생성자는 에러 코드와 메시지를 유지하고 빈 로그 컨텍스트를 제공한다")
    void notFoundLegacyConstructor() {
        var notFound = new NotFoundException(ErrorCode.MEMBER_NOT_FOUND, "회원을 찾을 수 없음");

        assertThat(notFound.getCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
        assertThat(notFound.getMessage()).isEqualTo("회원을 찾을 수 없음");
        assertThat(notFound.getLogContext()).isEmpty();
    }

    @Test
    @DisplayName("ForbiddenException 기존 생성자는 에러 코드와 메시지를 유지하고 빈 로그 컨텍스트를 제공한다")
    void forbiddenLegacyConstructor() {
        var forbidden = new ForbiddenException(ErrorCode.NOT_CLUB_MEMBER, "모임 접근 불가");

        assertThat(forbidden.getCode()).isEqualTo(ErrorCode.NOT_CLUB_MEMBER);
        assertThat(forbidden.getMessage()).isEqualTo("모임 접근 불가");
        assertThat(forbidden.getLogContext()).isEmpty();
    }

    @Test
    @DisplayName("UnauthorizedException 기존 생성자는 에러 코드와 메시지를 유지하고 빈 로그 컨텍스트를 제공한다")
    void unauthorizedLegacyConstructor() {
        var unauthorized = new UnauthorizedException(ErrorCode.UNAUTHORIZED, "인증 실패");

        assertThat(unauthorized.getCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
        assertThat(unauthorized.getMessage()).isEqualTo("인증 실패");
        assertThat(unauthorized.getLogContext()).isEmpty();
    }

    @Test
    @DisplayName("로그 컨텍스트는 생성 시점의 불변 복사본으로 보관한다")
    void immutableLogContext() {
        var source = new HashMap<>(Map.of("memberId", "1"));
        var badRequest = new BadRequestException(ErrorCode.INVALID_REQUEST, "잘못된 요청", source);
        var notFound = new NotFoundException(ErrorCode.MEMBER_NOT_FOUND, "회원을 찾을 수 없음", source);
        var forbidden = new ForbiddenException(ErrorCode.NOT_CLUB_MEMBER, "모임 접근 불가", source);
        var unauthorized = new UnauthorizedException(ErrorCode.UNAUTHORIZED, "인증 실패", source);

        source.put("memberId", "2");

        assertImmutableSnapshot(badRequest.getLogContext());
        assertImmutableSnapshot(notFound.getLogContext());
        assertImmutableSnapshot(forbidden.getLogContext());
        assertImmutableSnapshot(unauthorized.getLogContext());
    }

    private void assertImmutableSnapshot(Map<String, String> logContext) {
        assertThat(logContext).containsExactly(Map.entry("memberId", "1"));
        assertThatThrownBy(() -> logContext.put("clubId", "3"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
