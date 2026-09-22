package yeobaek.backend.book.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import yeobaek.backend.book.domain.vo.AuthorName;

class ResolvedAuthorsTest {

    @Test
    @DisplayName("저장되지 않은 작가가 포함되었는지 확인한다")
    void containsUnsavedAuthor() {
        Author saved = authorWithId(1L, "현진건");
        Author unsaved = new Author(new AuthorName("이상"));
        ResolvedAuthors authors = new ResolvedAuthors(List.of(saved, unsaved));

        assertThat(authors.containsUnsavedAuthor()).isTrue();
    }

    @Test
    @DisplayName("저장된 작가 id 집합을 반환한다")
    void ids() {
        Author first = authorWithId(1L, "현진건");
        Author second = authorWithId(2L, "이상");
        ResolvedAuthors authors = new ResolvedAuthors(List.of(first, second));

        assertThat(authors.ids()).containsExactlyInAnyOrder(1L, 2L);
    }

    private Author authorWithId(Long id, String name) {
        Author author = new Author(new AuthorName(name));
        ReflectionTestUtils.setField(author, "id", id);
        return author;
    }
}
