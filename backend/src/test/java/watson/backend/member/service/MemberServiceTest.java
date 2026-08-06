package watson.backend.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import watson.backend.member.domain.Member;
import watson.backend.member.dto.MemberCreateResponse;
import watson.backend.member.repository.MemberRepository;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberService memberService;

    @Test
    @DisplayName("회원을 생성하면 발급된 ID와 닉네임을 반환한다")
    void create() {
        Member saved = new Member("민서");
        ReflectionTestUtils.setField(saved, "id", 1L);
        given(memberRepository.save(any(Member.class))).willReturn(saved);

        MemberCreateResponse response = memberService.create("민서");

        assertThat(response.memberId()).isEqualTo(1L);
        assertThat(response.nickname()).isEqualTo("민서");
    }

    @Test
    @DisplayName("닉네임이 유효하지 않으면 저장 없이 예외가 전파된다")
    void invalidNicknameNotSaved() {
        assertThatThrownBy(() -> memberService.create(" "))
                .isInstanceOf(IllegalArgumentException.class);

        verify(memberRepository, never()).save(any());
    }
}
