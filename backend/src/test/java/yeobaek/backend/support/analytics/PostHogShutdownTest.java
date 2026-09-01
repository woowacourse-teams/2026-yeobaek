package yeobaek.backend.support.analytics;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.posthog.server.PostHogInterface;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class PostHogShutdownTest {

    @Test
    @DisplayName("종료할 때 대기 중인 이벤트를 전송한 뒤 SDK를 닫는다")
    void flushBeforeClose() {
        PostHogInterface postHog = mock(PostHogInterface.class);
        try (PostHogShutdown ignored = new PostHogShutdown(postHog)) {
            // try-with-resources 종료 시점의 호출 순서를 검증한다.
        }

        InOrder inOrder = inOrder(postHog);
        inOrder.verify(postHog).flush();
        inOrder.verify(postHog).close();
    }

    @Test
    @DisplayName("대기 이벤트 전송이 실패해도 SDK를 닫는다")
    void closeWhenFlushFails() {
        PostHogInterface postHog = mock(PostHogInterface.class);
        doThrow(new IllegalStateException("flush failed")).when(postHog).flush();

        assertThatThrownBy(() -> {
            try (PostHogShutdown ignored = new PostHogShutdown(postHog)) {
                // try-with-resources 종료 시점의 예외와 자원 정리를 검증한다.
            }
        }).isInstanceOf(IllegalStateException.class);

        verify(postHog).close();
    }
}
