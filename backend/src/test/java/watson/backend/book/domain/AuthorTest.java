package watson.backend.book.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AuthorTest {

    @Test
    @DisplayName("ISNI의 공백과 하이픈을 제거해 정규화한다")
    void normalizeIsni() {
        Author author = new Author("현진건", "0000 0001-2345 964X");

        assertThat(author.getIsni()).isEqualTo("000000012345964X");
    }

    @Test
    @DisplayName("ISNI 없이 이름만으로 생성할 수 있다")
    void createWithoutIsni() {
        Author author = new Author("작자 미상");

        assertThat(author.getIsni()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234", "000000012345964Y", "0000000123459640X"})
    @DisplayName("16자리(끝자리 X 허용) 형식이 아닌 ISNI는 거부한다")
    void rejectInvalidIsni(String invalidIsni) {
        assertThatThrownBy(() -> new Author("현진건", invalidIsni))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("작가 이름이 공백이거나 100자를 넘으면 거부한다")
    void rejectInvalidName() {
        assertThatThrownBy(() -> new Author(" "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Author("가".repeat(101)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("이름 일치 여부를 판단한다")
    void hasSameName() {
        Author author = new Author("현진건");

        assertThat(author.hasSameName("현진건")).isTrue();
        assertThat(author.hasSameName("이효석")).isFalse();
    }
}
