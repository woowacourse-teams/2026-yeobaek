package yeobaek.backend.club.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
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

class ClubMembersTest {

    private Club club;

    @BeforeEach
    void setUp() {
        Book book = new Book(new BookTitle("제목"), null, null, 1, null);
        club = new Club(new ClubName("1기"), book, new JoinCode("CODE01"));
    }

    @Test
    @DisplayName("회원 id에 해당하는 모임 참여 정보를 찾는다")
    void findByMemberId() {
        ClubMember first = new ClubMember(memberWithId(1L, "민서"), club);
        ClubMember second = new ClubMember(memberWithId(2L, "지수"), club);
        ClubMembers clubMembers = ClubMembers.from(List.of(first, second));

        assertThat(clubMembers.findByMemberId(2L)).contains(second);
    }

    @Test
    @DisplayName("회원 id에 해당하는 모임 참여 정보가 없으면 빈 값을 반환한다")
    void findByUnknownMemberId() {
        ClubMember clubMember = new ClubMember(memberWithId(1L, "민서"), club);
        ClubMembers clubMembers = ClubMembers.from(List.of(clubMember));

        assertThat(clubMembers.findByMemberId(2L)).isEmpty();
    }

    @Test
    @DisplayName("모임 참여 회원들의 id를 반환한다")
    void memberIds() {
        ClubMember first = new ClubMember(memberWithId(1L, "민서"), club);
        ClubMember second = new ClubMember(memberWithId(2L, "지수"), club);
        ClubMembers clubMembers = ClubMembers.from(List.of(first, second));

        assertThat(clubMembers.memberIds()).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("생성에 사용한 목록이 변경되어도 모임 참여 회원들은 유지된다")
    void copiesSourceMembers() {
        ClubMember clubMember = new ClubMember(memberWithId(1L, "민서"), club);
        List<ClubMember> source = new ArrayList<>(List.of(clubMember));
        ClubMembers clubMembers = ClubMembers.from(source);

        source.clear();

        assertThat(clubMembers.values()).containsExactly(clubMember);
    }

    private Member memberWithId(Long id, String nickname) {
        Member member = new Member(new Nickname(nickname));
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}
