package yeobaek.backend.space.club.domain;

import yeobaek.backend.space.club.persistence.Club;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import yeobaek.backend.space.api.club.ClubName;
import yeobaek.backend.space.api.club.JoinCode;
import yeobaek.backend.shared.identity.MemberId;

class ClubMembersTest {

    @Test
    @DisplayName("회원 id에 해당하는 가입 정보를 찾는다")
    void findByMemberId() {
        ClubMember first = membership(1L, 10L, "1기");
        ClubMember second = membership(2L, 10L, "1기");
        ClubMembers members = new ClubMembers(List.of(first, second));

        assertThat(members.findByMemberId(2L)).contains(second);
    }

    @Test
    @DisplayName("회원 id에 해당하는 가입 정보가 없으면 빈 값을 반환한다")
    void findByUnknownMemberId() {
        ClubMembers members = new ClubMembers(List.of(membership(1L, 10L, "1기")));

        assertThat(members.findByMemberId(2L)).isEmpty();
    }

    @Test
    @DisplayName("회원 id를 순서대로 반환한다")
    void memberIds() {
        ClubMembers members = new ClubMembers(List.of(
                membership(1L, 10L, "1기"),
                membership(2L, 10L, "1기")));

        assertThat(members.memberIds()).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("가입한 모임 id를 순서대로 반환한다")
    void clubIds() {
        ClubMembers members = new ClubMembers(List.of(
                membership(1L, 10L, "1기"),
                membership(1L, 20L, "2기")));

        assertThat(members.clubIds()).containsExactly(10L, 20L);
    }

    private ClubMember membership(Long memberId, Long clubId, String clubName) {
        Club club = new Club(1L, new ClubName(clubName), new JoinCode("CODE0" + clubId / 10));
        ReflectionTestUtils.setField(club, "id", clubId);
        return new ClubMember(new MemberId(memberId), club);
    }
}
