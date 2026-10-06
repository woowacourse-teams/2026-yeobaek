package yeobaek.backend.space.club.domain;

import yeobaek.backend.space.club.persistence.Club;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import yeobaek.backend.space.api.club.ClubName;
import yeobaek.backend.space.api.club.JoinCode;
import yeobaek.backend.shared.identity.MemberId;

class ClubMemberTest {

    @Test
    @DisplayName("모임 참여 정보는 참여 중 상태로 생성된다")
    void statusStartsJoined() {
        ClubMember membership = membership();
        assertThat(membership.getStatus()).isEqualTo(ClubMemberStatus.JOINED);
        assertThat(membership.isJoined()).isTrue();
    }

    @Test
    @DisplayName("모임을 탈퇴한 후 재가입할 수 있다")
    void leaveAndRejoin() {
        ClubMember membership = membership();
        membership.leave();
        assertThat(membership.getStatus()).isEqualTo(ClubMemberStatus.LEFT);
        assertThat(membership.isJoined()).isFalse();
        membership.rejoin();
        assertThat(membership.getStatus()).isEqualTo(ClubMemberStatus.JOINED);
        assertThat(membership.isJoined()).isTrue();
    }

    @Test
    @DisplayName("본인의 회원 id를 전달하면 참을 반환한다")
    void isOwnedByWhenIdMatches() {
        assertThat(membership().isOwnedBy(1L)).isTrue();
    }

    @Test
    @DisplayName("다른 회원의 id를 전달하면 거짓을 반환한다")
    void isNotOwnedByWhenIdDiffers() {
        assertThat(membership().isOwnedBy(2L)).isFalse();
    }

    private ClubMember membership() {
        return new ClubMember(new MemberId(1L), new Club(1L, new ClubName("1기"), new JoinCode("CODE01")));
    }
}
