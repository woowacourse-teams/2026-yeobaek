package yeobaek.backend.book.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class BodyCharacterCountTest {

    @ParameterizedTest
    @MethodSource("paragraphCharacterCounts")
    @DisplayName("문단의 내용을 이용해 본문 글자 수를 계산한다")
    void createsFromParagraphs(List<String> paragraphs, long expectedCount) {
        BodyCharacterCount count = BodyCharacterCount.fromParagraphs(paragraphs.stream());

        assertThat(count.value()).isEqualTo(expectedCount);
    }

    private static Stream<Arguments> paragraphCharacterCounts() {
        return Stream.of(
                Arguments.of(List.of("본문 내용", "다음 문단"), 10L),
                Arguments.of(List.of("e\u0301"), 1L),
                Arguments.of(List.of("e", "\u0301"), 2L),
                Arguments.of(List.of("😀"), 1L),
                Arguments.of(List.of("a \r\n\tb!"), 4L),
                Arguments.of(List.of("e\n\u0301"), 2L),
                Arguments.of(List.of("e\u0301 😀! \r\n\t"), 5L),
                Arguments.of(List.of("\u1100\u1161"), 1L));
    }

    @ParameterizedTest
    @CsvSource({"1,1", "699,1", "700,1", "701,2", "1400,2", "198376,284",
            "9223372036854775807,13176245766935395"})
    @DisplayName("본문 글자 수를 700으로 나누고 올림하여 쪽수를 계산한다")
    void estimatePagesWithoutOverflow(long characters, long pages) {
        assertThat(new BodyCharacterCount(characters).pageCount()).isEqualTo(pages);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, Long.MIN_VALUE})
    @DisplayName("본문 글자 수는 1 이상이어야 한다")
    void rejectNonPositiveCount(long count) {
        assertThatThrownBy(() -> new BodyCharacterCount(count)).isInstanceOf(IllegalArgumentException.class);
    }
}
