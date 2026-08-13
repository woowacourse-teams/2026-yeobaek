package watson.backend.member.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import watson.backend.member.domain.Member;
import watson.backend.support.RepositoryTest;

class MemberRepositoryTest extends RepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("회원을 저장하고 다시 조회할 수 있다")
    void saveAndFind() {
        Member saved = memberRepository.save(new Member("민서"));

        Member found = memberRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getNickname()).isEqualTo("민서");
    }

    @Test
    @DisplayName("이미 저장된 닉네임의 존재 여부를 확인한다")
    void existsByNickname() {
        memberRepository.save(new Member("민서"));

        assertThat(memberRepository.existsByNickname("민서")).isTrue();
        assertThat(memberRepository.existsByNickname("지수")).isFalse();
    }
}
