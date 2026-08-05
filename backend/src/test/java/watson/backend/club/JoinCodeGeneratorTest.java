package watson.backend.club;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;

class JoinCodeGeneratorTest {

    private final JoinCodeGenerator generator = new JoinCodeGenerator();

    @RepeatedTest(100)
    @DisplayName("참여 코드는 6자 대문자·숫자로 생성된다")
    void generateSixAlphanumericCode() {
        assertThat(generator.generate()).matches("^[A-Z0-9]{6}$");
    }
}
