package yeobaek.backend.member.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NicknameTest {

    @Test
    @DisplayName("닉네임은 값으로 동등성을 비교한다")
    void hasValueEquality() {
        assertThat(new Nickname("민서")).isEqualTo(new Nickname("민서"));
    }

    @Test
    @DisplayName("닉네임은 공백 문자열을 허용하지 않는다")
    void rejectsBlankValue() {
        assertThatThrownBy(() -> new Nickname(" ")).isInstanceOf(IllegalArgumentException.class);
    }
}
