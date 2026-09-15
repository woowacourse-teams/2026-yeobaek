package yeobaek.backend.club.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class ClubValueObjectTest {

    @Test
    @DisplayName("같은 모임 이름과 참여 코드는 같은 값이다")
    void clubValuesHaveValueEquality() {
        assertThat(new ClubName("함께 읽기")).isEqualTo(new ClubName("함께 읽기"));
        assertThat(new JoinCode("CODE01")).isEqualTo(new JoinCode("CODE01"));
    }

    @Test
    @DisplayName("참여 코드는 대문자와 숫자로 된 6자리여야 한다")
    void joinCodeRequiresIssuedFormat() {
        assertThatThrownBy(() -> new JoinCode("invalid"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @RepeatedTest(100)
    @DisplayName("참여 코드는 6자 대문자·숫자로 생성된다")
    void generateSixAlphanumericCode() {
        assertThat(JoinCode.generate().value()).matches("^[A-Z0-9]{6}$");
    }

    @Test
    @DisplayName("진도율은 최근 본문 순서를 전체 본문 수로 나누어 반올림한다")
    void progressRateRoundsReadingRatio() {
        assertThat(ProgressRate.calculate(2, 3).roundedPercentage()).isEqualTo(67);
    }

    @Test
    @DisplayName("동일한 독서 비율은 같은 진도율 값이다")
    void equivalentRatiosHaveValueEquality() {
        assertThat(ProgressRate.calculate(1, 2)).isEqualTo(ProgressRate.calculate(2, 4));
    }

    @Test
    @DisplayName("진도율은 기존 계산 순서를 유지한 뒤 반올림한다")
    void preservesPercentageCalculationOrder() {
        assertThat(ProgressRate.calculate(57, 200).roundedPercentage()).isEqualTo(29);
    }
}
