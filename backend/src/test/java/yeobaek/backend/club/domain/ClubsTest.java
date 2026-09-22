package yeobaek.backend.club.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.club.domain.vo.ClubName;
import yeobaek.backend.club.domain.vo.JoinCode;

class ClubsTest {

    @Test
    @DisplayName("모임이 없으면 빈 컬렉션이다")
    void isEmpty() {
        assertThat(new Clubs(List.of()).isEmpty()).isTrue();
    }

    @Test
    @DisplayName("모임 id를 순서대로 반환한다")
    void ids() {
        Clubs clubs = new Clubs(List.of(clubWithId(2L, "둘째 모임"), clubWithId(1L, "첫 모임")));

        assertThat(clubs.ids()).containsExactly(2L, 1L);
    }

    @Test
    @DisplayName("원본 목록을 복사하고 변경할 수 없는 모임 목록을 반환한다")
    void copyAndExposeUnmodifiableList() {
        Club club = clubWithId(1L, "첫 모임");
        List<Club> source = new ArrayList<>(List.of(club));
        Clubs clubs = new Clubs(source);

        source.clear();

        assertThat(clubs.asList()).containsExactly(club);
        assertThatThrownBy(() -> clubs.asList().add(club))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private Club clubWithId(Long id, String name) {
        Book book = new Book(new BookTitle("도서"), null, null, 1, null);
        Club club = new Club(new ClubName(name), book, new JoinCode("CLUB01"));
        ReflectionTestUtils.setField(club, "id", id);
        return club;
    }
}
