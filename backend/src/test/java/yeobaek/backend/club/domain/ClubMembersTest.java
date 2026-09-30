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

class ClubMembersTest {

    @Test
    @DisplayName("회원 id에 해당하는 가입 정보를 찾는다")
    void findByMemberId() {
        ClubMember first = membership(1L, 10L, 100L, "1기");
        ClubMember second = membership(2L, 10L, 100L, "1기");
        ClubMembers members = new ClubMembers(List.of(first, second));

        assertThat(members.findByMemberId(2L)).contains(second);
    }

    @Test
    @DisplayName("회원 id에 해당하는 가입 정보가 없으면 빈 값을 반환한다")
    void findByUnknownMemberId() {
        ClubMembers members = new ClubMembers(List.of(membership(1L, 10L, 100L, "1기")));

        assertThat(members.findByMemberId(2L)).isEmpty();
    }

    @Test
    @DisplayName("회원 id를 순서대로 반환한다")
    void memberIds() {
        ClubMembers members = new ClubMembers(List.of(
                membership(1L, 10L, 100L, "1기"),
                membership(2L, 10L, 100L, "1기")));

        assertThat(members.memberIds()).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("가입한 모임 id를 순서대로 반환한다")
    void clubIds() {
        ClubMembers members = new ClubMembers(List.of(
                membership(1L, 10L, 100L, "1기"),
                membership(1L, 20L, 200L, "2기")));

        assertThat(members.clubIds()).containsExactly(10L, 20L);
    }

    @Test
    @DisplayName("가입한 모임의 중복 없는 도서 id를 반환한다")
    void bookIds() {
        ClubMembers members = new ClubMembers(List.of(
                membership(1L, 10L, 100L, "1기"),
                membership(1L, 20L, 100L, "2기"),
                membership(1L, 30L, 200L, "3기")));

        assertThat(members.bookIds()).containsExactly(100L, 200L);
    }

    private ClubMember membership(Long memberId, Long clubId, Long bookId, String clubName) {
        Book book = new Book(new BookTitle("제목"), null, null, 1, null);
        ReflectionTestUtils.setField(book, "id", bookId);
        Club club = new Club(new ClubName(clubName), book, new JoinCode("CODE0" + clubId / 10));
        ReflectionTestUtils.setField(club, "id", clubId);
        Member member = new Member(new Nickname("민서"));
        ReflectionTestUtils.setField(member, "id", memberId);
        return new ClubMember(member, club);
    }
}
