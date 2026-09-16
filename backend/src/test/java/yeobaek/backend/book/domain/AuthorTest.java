package yeobaek.backend.book.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import yeobaek.backend.book.domain.vo.AuthorName;
import yeobaek.backend.book.domain.vo.Isni;

class AuthorTest {

    @Test
    @DisplayName("작가는 생성 시 전달받은 ISNI 값 객체를 유지한다")
    void retainsIsniValueObject() {
        Isni isni = new Isni("0000 0001-2345 964X");
        Author author = new Author("현진건", isni);

        assertThat(author.getIsni()).isEqualTo(isni);
    }

    @Test
    @DisplayName("ISNI 없이 이름만으로 생성할 수 있다")
    void createWithoutIsni() {
        Author author = new Author("작자 미상");

        assertThat(author.getIsni()).isNull();
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

        assertThat(author.hasSameName(new AuthorName("현진건"))).isTrue();
        assertThat(author.hasSameName(new AuthorName("이효석"))).isFalse();
    }
}
