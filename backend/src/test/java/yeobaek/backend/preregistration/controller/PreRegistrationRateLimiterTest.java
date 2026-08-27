package yeobaek.backend.preregistration.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.TooManyRequestsException;

class PreRegistrationRateLimiterTest {

    private Instant now;
    private PreRegistrationRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        now = Instant.parse("2026-08-27T00:00:00Z");
        rateLimiter = new PreRegistrationRateLimiter();
    }

    @Test
    @DisplayName("같은 IP는 1분 동안 다섯 번 허용하고 여섯 번째 요청을 차단한다")
    void allowFiveRequestsAndRejectSixth() {
        for (int requestCount = 0; requestCount < 5; requestCount++) {
            rateLimiter.check(testIpv4(1), now);
        }

        assertThatThrownBy(() -> rateLimiter.check(testIpv4(1), now))
                .isInstanceOf(TooManyRequestsException.class)
                .hasMessage("요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.")
                .extracting("code")
                .isEqualTo(ErrorCode.RATE_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("고정 1분 창이 지나면 같은 IP의 요청 횟수를 초기화한다")
    void resetAfterOneMinute() {
        for (int requestCount = 0; requestCount < 5; requestCount++) {
            rateLimiter.check(testIpv4(1), now);
        }
        now = now.plus(Duration.ofMinutes(1));

        assertThatCode(() -> rateLimiter.check(testIpv4(1), now))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("서로 다른 IP의 요청 횟수는 독립적으로 계산한다")
    void separateDifferentClientIps() {
        for (int requestCount = 0; requestCount < 5; requestCount++) {
            rateLimiter.check(testIpv4(1), now);
        }

        assertThatCode(() -> rateLimiter.check(testIpv4(2), now))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("새 요청이 들어오면 만료된 IP 상태를 지연 정리한다")
    void lazilyRemoveExpiredClientState() {
        rateLimiter.check(testIpv4(1), now);
        now = now.plus(Duration.ofMinutes(1));

        rateLimiter.check(testIpv4(2), now);

        assertThat(rateLimiter.trackedClientCount()).isEqualTo(1);
    }

    private static String testIpv4(int host) {
        return "198.51.100." + host;
    }
}
