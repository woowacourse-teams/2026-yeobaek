package yeobaek.backend.member.internal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.member.api.MemberFailureReason;
import yeobaek.backend.member.api.MemberNotFoundFailure;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.MemberProfile;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.repository.MemberRepository;

@ExtendWith(MockitoExtension.class)
class MemberQueryServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberQueryService memberQueryService;

    @Test
    @DisplayName("회원 조회 결과는 영속성 모델을 노출하지 않는 프로필이다")
    void getProfile() {
        Member member = member(1L, "민서");
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        MemberProfile profile = memberQueryService.getProfile(new MemberId(1L));

        assertThat(profile).isEqualTo(new MemberProfile(new MemberId(1L), "민서"));
    }

    @Test
    @DisplayName("존재하지 않는 회원 조회는 회원 모듈 실패 사유를 제공한다")
    void rejectUnknownMember() {
        given(memberRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberQueryService.getProfile(new MemberId(99L)))
                .isInstanceOf(MemberNotFoundFailure.class)
                .satisfies(failure -> {
                    MemberNotFoundFailure memberFailure = (MemberNotFoundFailure) failure;
                    assertThat(memberFailure.reason()).isEqualTo(MemberFailureReason.MEMBER_NOT_FOUND);
                    assertThat(memberFailure.memberId()).isEqualTo(new MemberId(99L));
                });
    }

    @Test
    @DisplayName("여러 회원 조회는 존재하는 프로필만 식별자로 반환한다")
    void findProfiles() {
        Member first = member(1L, "민서");
        Member second = member(2L, "지수");
        given(memberRepository.findAllById(List.of(1L, 2L, 99L))).willReturn(List.of(first, second));

        Map<MemberId, MemberProfile> profiles = memberQueryService.findProfiles(
                Set.of(new MemberId(1L), new MemberId(2L), new MemberId(99L)));

        assertThat(profiles).containsOnly(
                Map.entry(new MemberId(1L), new MemberProfile(new MemberId(1L), "민서")),
                Map.entry(new MemberId(2L), new MemberProfile(new MemberId(2L), "지수")));
        assertThatThrownBy(() -> profiles.clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    private Member member(Long id, String nickname) {
        Member member = new Member(new Nickname(nickname));
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}
