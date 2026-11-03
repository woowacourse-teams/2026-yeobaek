package yeobaek.backend.member.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import yeobaek.backend.member.domain.vo.Nickname;

class MemberTest {

    @Test
    @DisplayName("1~20자 닉네임으로 회원을 생성할 수 있다")
    void createWithValidNickname() {
        assertThatCode(() -> new Member(new Nickname("민")))
                .doesNotThrowAnyException();
        assertThat(new Member(new Nickname("가".repeat(20))).getNickname()).hasSize(20);
    }

    @Test
    @DisplayName("닉네임을 변경할 수 있다")
    void changeNickname() {
        Member member = new Member(new Nickname("왓슨"));

        member.changeNickname(new Nickname("새닉네임"));

        assertThat(member.getNickname()).isEqualTo("새닉네임");
    }

    @Test
    @DisplayName("null로 닉네임을 변경하면 예외가 발생하고 기존 닉네임을 유지한다")
    void rejectNullNicknameOnChange() {
        Member member = new Member(new Nickname("왓슨"));

        assertThatThrownBy(() -> member.changeNickname(null)).isInstanceOf(IllegalArgumentException.class);
        assertThat(member.getNickname()).isEqualTo("왓슨");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "   "})
    @DisplayName("닉네임이 없거나 공백뿐이면 회원 생성에 실패한다")
    void rejectBlankNickname(String nickname) {
        assertThatThrownBy(() -> new Member(new Nickname(nickname)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("닉네임이 20자를 넘으면 회원 생성에 실패한다")
    void rejectTooLongNickname() {
        assertThatThrownBy(() -> new Member(new Nickname("가".repeat(21))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
