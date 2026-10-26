package yeobaek.backend.book.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class BookDescriptionTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\r\n\t", "\u2003"})
    @DisplayName("책 소개는 null이나 공백 문자열을 허용하지 않는다")
    void rejectsNullOrBlankDescription(String value) {
        assertThatThrownBy(() -> new BookDescription(value)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("책 소개의 앞뒤 공백을 제거하고 내부 내용은 보존한다")
    void stripOuterWhitespaceAndPreserveInnerContent() {
        assertThat(new BookDescription(" \n첫 줄\n둘째 줄.\t "))
                .isEqualTo(new BookDescription("첫 줄\n둘째 줄."));
    }
}
