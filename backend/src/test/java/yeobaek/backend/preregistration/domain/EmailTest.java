package yeobaek.backend.preregistration.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EmailTest {

    @Test
    @DisplayName("이메일은 정규화한 값으로 동등성을 비교한다")
    void hasEqualityAfterNormalization() {
        assertThat(new Email(" Reader@Example.com ")).isEqualTo(new Email("reader@example.com"));
    }
}
