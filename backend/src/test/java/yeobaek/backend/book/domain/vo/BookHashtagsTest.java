package yeobaek.backend.book.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BookHashtagsTest {

    @Test
    @DisplayName("다섯 개·열 글자 경계와 입력 순서·중복·기호를 그대로 보존한다")
    void preservesValidBoundaryValues() {
        List<String> input = new ArrayList<>(List.of("첫사랑", "#반전", "첫사랑", " 가벼운감상 ", "가나다라마바사아자차"));
        BookHashtags hashtags = new BookHashtags(input);
        input.clear();

        assertThat(hashtags.value()).containsExactly("첫사랑", "#반전", "첫사랑", " 가벼운감상 ", "가나다라마바사아자차");
        assertThatThrownBy(() -> hashtags.value().add("추가")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("보조 평면 문자는 UTF-16 길이 대신 유니코드 문자 수로 제한한다")
    void countsUnicodeCodePoints() {
        String tenCharacters = "😀".repeat(10);

        assertThat(new BookHashtags(List.of(tenCharacters)).value()).containsExactly(tenCharacters);
        assertThatThrownBy(() -> new BookHashtags(List.of("😀".repeat(11))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("여섯 개 또는 열한 글자 해시태그를 거절한다")
    void rejectsOverflow() {
        assertThatThrownBy(() -> new BookHashtags(List.of("가", "나", "다", "라", "마", "바")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BookHashtags(List.of("가나다라마바사아자차카")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("빈 목록은 선택 메타데이터 제거 요청에 사용할 수 있다")
    void acceptsEmptyList() {
        assertThat(new BookHashtags(List.of()).value()).isEmpty();
    }
}
