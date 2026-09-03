package yeobaek.backend.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.dto.BlockedMembersResponse;
import yeobaek.backend.member.repository.MemberBlockRepository;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.support.BadRequestException;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.IntegrationTest;
import yeobaek.backend.support.NotFoundException;

class MemberBlockServiceTest extends IntegrationTest {

    @Autowired
    private MemberBlockService memberBlockService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberBlockRepository memberBlockRepository;

    private Member blocker;
    private Member blocked;

    @BeforeEach
    void setUp() {
        blocker = memberRepository.save(new Member("민서"));
        blocked = memberRepository.save(new Member("지수"));
    }

    @Test
    @DisplayName("회원을 차단하고 차단 목록에서 조회한다")
    void blockAndFind() {
        memberBlockService.block(blocker.getId(), blocked.getId());

        BlockedMembersResponse response = memberBlockService.findBlockedMembers(blocker.getId());

        assertThat(response.blockedMembers()).singleElement().satisfies(member -> {
            assertThat(member.memberId()).isEqualTo(blocked.getId());
            assertThat(member.nickname()).isEqualTo("지수");
        });
    }

    @Test
    @DisplayName("같은 회원을 다시 차단해도 관계는 하나만 유지된다")
    void blockIdempotently() {
        memberBlockService.block(blocker.getId(), blocked.getId());
        memberBlockService.block(blocker.getId(), blocked.getId());

        assertThat(memberBlockRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("관계가 없을 때도 차단 해제는 멱등하게 성공한다")
    void unblockIdempotently() {
        memberBlockService.unblock(blocker.getId(), blocked.getId());
        memberBlockService.block(blocker.getId(), blocked.getId());
        memberBlockService.unblock(blocker.getId(), blocked.getId());
        memberBlockService.unblock(blocker.getId(), blocked.getId());

        assertThat(memberBlockRepository.count()).isZero();
    }

    @Test
    @DisplayName("자기 자신을 차단하면 CANNOT_BLOCK_SELF로 실패한다")
    void rejectSelfBlock() {
        assertThatThrownBy(() -> memberBlockService.block(blocker.getId(), blocker.getId()))
                .isInstanceOf(BadRequestException.class)
                .extracting("code").isEqualTo(ErrorCode.CANNOT_BLOCK_SELF);
    }

    @Test
    @DisplayName("존재하지 않는 회원은 차단하거나 차단 해제할 수 없다")
    void rejectUnknownMember() {
        assertThatThrownBy(() -> memberBlockService.block(blocker.getId(), 999L))
                .isInstanceOf(NotFoundException.class)
                .extracting("code").isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
        assertThatThrownBy(() -> memberBlockService.unblock(blocker.getId(), 999L))
                .isInstanceOf(NotFoundException.class)
                .extracting("code").isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("차단은 단방향 관계다")
    void blockIsDirectional() {
        memberBlockService.block(blocker.getId(), blocked.getId());

        assertThat(memberBlockService.findBlockedMembers(blocked.getId()).blockedMembers()).isEmpty();
    }
}
