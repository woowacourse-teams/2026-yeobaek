package yeobaek.backend.club.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.club.domain.vo.ClubName;
import yeobaek.backend.club.domain.vo.JoinCode;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;

class MemberClubMembershipsTest {

    @Test
    @DisplayName("회원이 가입한 모임 id를 순서대로 반환한다")
    void clubIds() {
        MemberClubMemberships memberships = new MemberClubMemberships(List.of(
                membership(1L, 10L, "1기"),
                membership(2L, 20L, "2기")));

        assertThat(memberships.clubIds()).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("회원이 가입한 모임의 중복 없는 도서 id를 반환한다")
    void bookIds() {
        MemberClubMemberships memberships = new MemberClubMemberships(List.of(
                membership(1L, 10L, "1기"),
                membership(2L, 10L, "2기"),
                membership(3L, 20L, "3기")));

        assertThat(memberships.bookIds()).containsExactly(10L, 20L);
    }

    private ClubMember membership(Long clubId, Long bookId, String clubName) {
        Book book = new Book(new BookTitle("제목"), null, null, 1, null);
        ReflectionTestUtils.setField(book, "id", bookId);
        Club club = new Club(new ClubName(clubName), book, new JoinCode("CODE0" + clubId));
        ReflectionTestUtils.setField(club, "id", clubId);
        return new ClubMember(new Member(new Nickname("민서")), club);
    }
}
