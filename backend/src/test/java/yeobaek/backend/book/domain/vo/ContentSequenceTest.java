package yeobaek.backend.book.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ContentSequenceTest {

    @ParameterizedTest
    @CsvSource({"1, false", "2, false", "3, true"})
    void isAfter(int sequence, boolean expected) {
        assertThat(new ContentSequence(sequence).isAfter(new ContentSequence(2))).isEqualTo(expected);
    }
}
