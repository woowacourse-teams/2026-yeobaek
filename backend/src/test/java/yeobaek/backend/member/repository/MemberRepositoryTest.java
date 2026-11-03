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
    @DisplayName("이미 저장된 닉네임의 존재 여부를 확인한다")
    void existsByNickname() {
        memberRepository.save(new Member(new Nickname("민서")));

        assertThat(memberRepository.existsByNickname("민서")).isTrue();
        assertThat(memberRepository.existsByNickname("지수")).isFalse();
    }

    @Test
    @DisplayName("DB는 대소문자만 다른 닉네임도 중복으로 거절한다")
    void rejectDuplicateNicknameAtDatabase() {
        memberRepository.saveAndFlush(new Member(new Nickname("Reader")));

        assertThatThrownBy(() -> memberRepository.saveAndFlush(new Member(new Nickname("reader"))))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(memberRepository.count()).isOne();
    }

    @Test
    @DisplayName("닉네임 중복 검사에서 요청 회원 자신은 제외한다")
    void existsByNicknameExcludingMember() {
        Member member = memberRepository.save(new Member(new Nickname("Reader")));
        Member other = memberRepository.save(new Member(new Nickname("다른회원")));

        assertThat(memberRepository.existsByNicknameExcludingMember("reader", member.getId())).isFalse();
        assertThat(memberRepository.existsByNicknameExcludingMember("reader", other.getId())).isTrue();
    }
}
