package yeobaek.backend.member.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.support.IntegrationTest;

class MemberRepositoryTest extends IntegrationTest {

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("회원을 저장하고 다시 조회할 수 있다")
    void saveAndFind() {
        Member saved = memberRepository.save(new Member(new Nickname("민서")));

        Member found = memberRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getNickname()).isEqualTo("민서");
    }

    @Test
    @DisplayName("DB는 대소문자만 다른 닉네임도 중복으로 거절한다")
    void rejectDuplicateNicknameAtDatabase() {
        memberRepository.saveAndFlush(new Member(new Nickname("Reader")));

        assertThatThrownBy(() -> memberRepository.saveAndFlush(new Member(new Nickname("reader"))))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(memberRepository.count()).isOne();
    }
}
