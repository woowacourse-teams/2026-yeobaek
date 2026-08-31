package yeobaek.backend.preregistration.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class PreRegistrationTest {

    @Test
    @DisplayName("이메일의 앞뒤 공백을 제거하고 소문자로 정규화한다")
    void normalizeEmail() {
        PreRegistration preRegistration = new PreRegistration("  Reader.Name+Demo@Example.COM  ");

        assertThat(preRegistration.getEmail()).isEqualTo("reader.name+demo@example.com");
        assertThat(preRegistration.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("254자인 올바른 이메일을 허용한다")
    void allowMaximumLengthEmail() {
        String email = "a".repeat(64) + "@" + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(61);

        assertThat(new PreRegistration(email).getEmail()).hasSize(254);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "reader", "reader@", "@example.com", ".reader@example.com",
            "reader.@example.com", "reader..name@example.com", "reader@example", "reader@-example.com"})
    @DisplayName("일반적인 이메일 형식이 아니면 생성에 실패한다")
    void rejectInvalidEmail(String email) {
        assertThatThrownBy(() -> new PreRegistration(email))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("올바른 이메일 주소를 입력해 주세요.");
    }

    @Test
    @DisplayName("정규화 후 254자를 넘는 이메일은 생성에 실패한다")
    void rejectTooLongEmail() {
        String email = "a".repeat(65) + "@" + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(61);

        assertThatThrownBy(() -> new PreRegistration(email))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("올바른 이메일 주소를 입력해 주세요.");
    }
}
