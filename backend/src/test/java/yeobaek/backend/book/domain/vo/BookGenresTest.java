package yeobaek.backend.book.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BookGenresTest {

    @Test
    @DisplayName("복수 자유 장르의 순서·중복·문구를 그대로 보존하고 개수·길이를 제한하지 않는다")
    void preservesFreeFormGenres() {
        List<String> input = new ArrayList<>(List.of("로맨스", "비극", "로맨스", " 자유 장르 ",
                "아주 긴 장르 표현".repeat(20), "성장", "풍자"));
        List<String> expected = List.copyOf(input);

        BookGenres genres = new BookGenres(input);
        input.clear();

        assertThat(genres.value()).containsExactlyElementsOf(expected);
        assertThatThrownBy(() -> genres.value().add("추가")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("빈 장르 목록은 선택 정보 제거 요청에 사용할 수 있다")
    void acceptsEmptyList() {
        assertThat(new BookGenres(List.of()).value()).isEmpty();
    }
}
