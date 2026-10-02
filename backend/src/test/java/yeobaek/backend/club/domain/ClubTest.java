package yeobaek.backend.club.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import yeobaek.backend.club.domain.vo.ClubName;
import yeobaek.backend.club.domain.vo.JoinCode;

class ClubTest {

    @Test
    @DisplayName("1~20자 이름으로 모임을 생성할 수 있다")
    void createWithValidName() {
        assertThatCode(() -> new Club(1L, new ClubName("모"), new JoinCode("CODE01"))).doesNotThrowAnyException();
        assertThat(new Club(1L, new ClubName("가".repeat(20)), new JoinCode("CODE01")).getName()).hasSize(20);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "   "})
    @DisplayName("이름이 없거나 공백뿐이면 모임 생성에 실패한다")
    void rejectBlankName(String name) {
        assertThatThrownBy(() -> new Club(1L, new ClubName(name), new JoinCode("CODE01")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("이름이 20자를 넘으면 모임 생성에 실패한다")
    void rejectTooLongName() {
        assertThatThrownBy(() -> new Club(1L, new ClubName("가".repeat(21)), new JoinCode("CODE01")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
