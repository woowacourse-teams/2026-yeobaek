package yeobaek.backend.book.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BookValueObjectTest {

    @Test
    @DisplayName("같은 작가 이름과 ISNI는 같은 값이다")
    void authorValuesHaveValueEquality() {
        assertThat(new AuthorName("현진건")).isEqualTo(new AuthorName("현진건"));
        assertThat(new Isni("0000 0001-2345 964X")).isEqualTo(new Isni("000000012345964X"));
    }

    @Test
    @DisplayName("책과 목차의 이름 값은 공백 문자열을 허용하지 않는다")
    void titlesRejectBlankValues() {
        assertThatThrownBy(() -> new BookTitle(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ChapterTitle(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Publisher(" ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("문장 내용은 앞뒤 공백을 보존한다")
    void sentenceContentPreservesWhitespace() {
        SentenceContent content = new SentenceContent("  문장 내용  ");

        assertThat(content.value()).isEqualTo("  문장 내용  ");
    }

    @Test
    @DisplayName("콘텐츠 순서와 도서의 본문 개수는 1 이상이어야 한다")
    void sequenceAndPassageCountRequirePositiveValues() {
        assertThatThrownBy(() -> new ContentSequence(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PassageCount(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("본문 범위는 1 이상의 시작과 시작 이상인 끝을 가진다")
    void passageRangeOwnsInclusiveBounds() {
        assertThat(new PassageRange(3, 5).size()).isEqualTo(3);
        assertThatThrownBy(() -> new PassageRange(0, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PassageRange(2, 1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("DB 저장 한도보다 큰 텍스트도 문장 값 자체로는 유효하다")
    void sentenceContentDoesNotOwnDatabaseByteLimit() {
        String contentOverTextLimit = "가".repeat(21_846);

        assertThat(new SentenceContent(contentOverTextLimit).value()).isEqualTo(contentOverTextLimit);
    }

    @Test
    @DisplayName("본문 범위 값은 요청 한 번의 최대 조회 개수를 제한하지 않는다")
    void passageRangeDoesNotOwnRequestSizeLimit() {
        assertThat(new PassageRange(1, 101).size()).isEqualTo(101);
    }
}
