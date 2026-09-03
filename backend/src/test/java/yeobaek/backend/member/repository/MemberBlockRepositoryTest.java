package yeobaek.backend.member.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.MemberBlock;
import yeobaek.backend.support.IntegrationTest;

class MemberBlockRepositoryTest extends IntegrationTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberBlockRepository memberBlockRepository;

    @Test
    @DisplayName("차단 목록은 닉네임과 회원 ID 오름차순으로 조회된다")
    void findAllOrdered() {
        Member blocker = memberRepository.save(new Member("민서"));
        Member sameNicknameFirst = memberRepository.save(new Member("가람"));
        Member laterNickname = memberRepository.save(new Member("하늘"));
        Member sameNicknameSecond = memberRepository.save(new Member("가람"));
        memberBlockRepository.saveAll(List.of(
                new MemberBlock(blocker, laterNickname),
                new MemberBlock(blocker, sameNicknameSecond),
                new MemberBlock(blocker, sameNicknameFirst)));

        List<MemberBlock> blocks = memberBlockRepository.findAllWithBlockedByBlockerId(blocker.getId());

        assertThat(blocks).extracting(block -> block.getBlocked().getId())
                .containsExactly(sameNicknameFirst.getId(), sameNicknameSecond.getId(), laterNickname.getId());
    }

    @Test
    @DisplayName("차단자 계정이 삭제되면 DB cascade로 차단 관계가 삭제된다")
    void deleteByBlockerCascade() {
        Member blocker = memberRepository.save(new Member("민서"));
        Member blocked = memberRepository.save(new Member("지수"));
        memberBlockRepository.saveAndFlush(new MemberBlock(blocker, blocked));

        memberRepository.deleteById(blocker.getId());
        memberRepository.flush();

        assertThat(memberBlockRepository.count()).isZero();
    }

    @Test
    @DisplayName("차단당한 회원 계정이 삭제되면 DB cascade로 차단 관계가 삭제된다")
    void deleteByBlockedCascade() {
        Member blocker = memberRepository.save(new Member("민서"));
        Member blocked = memberRepository.save(new Member("지수"));
        memberBlockRepository.saveAndFlush(new MemberBlock(blocker, blocked));

        memberRepository.deleteById(blocked.getId());
        memberRepository.flush();

        assertThat(memberBlockRepository.count()).isZero();
    }
}
