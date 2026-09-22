package yeobaek.backend.club.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.club.domain.vo.ClubName;
import yeobaek.backend.club.domain.vo.JoinCode;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;

class JoinedClubMembersTest {

    private Club club;

    @BeforeEach
    void setUp() {
        Book book = new Book(new BookTitle("제목"), null, null, 1, null);
        club = new Club(new ClubName("1기"), book, new JoinCode("CODE01"));
    }

    @Test
    @DisplayName("회원 id에 해당하는 가입 정보를 찾는다")
    void findByMemberId() {
        ClubMember first = new ClubMember(memberWithId(1L, "민서"), club);
        ClubMember second = new ClubMember(memberWithId(2L, "지수"), club);
        JoinedClubMembers members = new JoinedClubMembers(List.of(first, second));

        assertThat(members.findByMemberId(2L)).contains(second);
    }

    @Test
    @DisplayName("회원 id에 해당하는 가입 정보가 없으면 빈 값을 반환한다")
    void findByUnknownMemberId() {
        ClubMember clubMember = new ClubMember(memberWithId(1L, "민서"), club);
        JoinedClubMembers members = new JoinedClubMembers(List.of(clubMember));

        assertThat(members.findByMemberId(2L)).isEmpty();
    }

    @Test
    @DisplayName("가입 중인 회원 id를 순서대로 반환한다")
    void memberIds() {
        ClubMember first = new ClubMember(memberWithId(1L, "민서"), club);
        ClubMember second = new ClubMember(memberWithId(2L, "지수"), club);
        JoinedClubMembers members = new JoinedClubMembers(List.of(first, second));

        assertThat(members.memberIds()).containsExactly(1L, 2L);
    }

    private Member memberWithId(Long id, String nickname) {
        Member member = new Member(new Nickname(nickname));
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}
