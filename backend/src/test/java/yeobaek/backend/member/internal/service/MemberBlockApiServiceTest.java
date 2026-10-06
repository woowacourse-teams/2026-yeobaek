package yeobaek.backend.member.internal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.member.api.MemberNotFoundException;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.member.api.block.SelfBlockException;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.MemberBlock;
import yeobaek.backend.member.domain.MemberProfile;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.repository.MemberBlockRepository;
import yeobaek.backend.member.repository.MemberRepository;

@ExtendWith(MockitoExtension.class)
class MemberBlockApiServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberBlockRepository memberBlockRepository;

    @InjectMocks
    private MemberBlockApiService memberBlockApiService;

    @Test
    @DisplayName("차단은 단방향 관계를 저장하고 같은 관계에는 멱등하다")
    void blockIdempotently() {
        Member blocker = member(1L, "민서");
        Member blocked = member(2L, "지수");
        given(memberRepository.findById(2L)).willReturn(Optional.of(blocked));
        given(memberRepository.getReferenceById(1L)).willReturn(blocker);
        given(memberBlockRepository.existsByBlockerIdAndBlockedId(1L, 2L)).willReturn(false, true);

        memberBlockApiService.block(new MemberId(1L), new MemberId(2L));
        memberBlockApiService.block(new MemberId(1L), new MemberId(2L));

        ArgumentCaptor<MemberBlock> blockCaptor = ArgumentCaptor.forClass(MemberBlock.class);
        verify(memberBlockRepository).save(blockCaptor.capture());
        assertThat(blockCaptor.getValue().getBlocker()).isSameAs(blocker);
        assertThat(blockCaptor.getValue().getBlocked()).isSameAs(blocked);
    }

    @Test
    @DisplayName("자기 자신 차단은 저장소 조회 전에 거부한다")
    void rejectSelfBlock() {
        assertThatThrownBy(() -> memberBlockApiService.block(new MemberId(1L), new MemberId(1L)))
                .isInstanceOf(SelfBlockException.class)
                .satisfies(failure -> assertThat(((SelfBlockException) failure).getCode())
                        .isEqualTo(ErrorCode.CANNOT_BLOCK_SELF));

        verify(memberRepository, never()).findById(1L);
        verify(memberBlockRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("존재하지 않는 차단 대상은 회원 모듈 실패로 거부한다")
    void rejectUnknownBlockedMember() {
        given(memberRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberBlockApiService.block(new MemberId(1L), new MemberId(99L)))
                .isInstanceOf(MemberNotFoundException.class)
                .satisfies(failure -> assertThat(((MemberNotFoundException) failure).memberId())
                        .isEqualTo(new MemberId(99L)));
    }

    @Test
    @DisplayName("차단 해제는 관계가 없어도 성공하고 존재하지 않는 대상만 거부한다")
    void unblockIdempotently() {
        given(memberRepository.existsById(2L)).willReturn(true);

        memberBlockApiService.unblock(new MemberId(1L), new MemberId(2L));

        verify(memberBlockRepository).deleteByBlockerIdAndBlockedId(1L, 2L);

        given(memberRepository.existsById(99L)).willReturn(false);
        assertThatThrownBy(() -> memberBlockApiService.unblock(new MemberId(1L), new MemberId(99L)))
                .isInstanceOf(MemberNotFoundException.class);
    }

    @Test
    @DisplayName("후보 집합에 대한 차단 결과는 차단자에서 대상으로 향하는 식별자만 반환한다")
    void findBlockedMemberIds() {
        given(memberBlockRepository.findBlockedMemberIds(1L, List.of(2L, 3L)))
                .willReturn(List.of(2L));

        Set<MemberId> blockedIds = memberBlockApiService.findBlockedMemberIds(
                new MemberId(1L), Set.of(new MemberId(2L), new MemberId(3L)));

        assertThat(blockedIds).containsExactly(new MemberId(2L));
    }

    @Test
    @DisplayName("차단 목록은 영속성 모델 대신 회원 프로필을 반환한다")
    void findBlockedProfiles() {
        Member blocker = member(1L, "민서");
        Member blocked = member(2L, "지수");
        given(memberBlockRepository.findAllWithBlockedByBlockerId(1L))
                .willReturn(List.of(new MemberBlock(blocker, blocked)));

        Map<MemberId, MemberProfile> profiles = memberBlockApiService.findBlockedProfiles(new MemberId(1L));

        assertThat(profiles).containsOnly(
                Map.entry(new MemberId(2L), new MemberProfile(new MemberId(2L), "지수")));
    }

    @Test
    @DisplayName("회원 소유 차단 데이터 삭제는 양방향 관계를 대상으로 한다")
    void eraseAllInvolving() {
        memberBlockApiService.eraseAllInvolving(new MemberId(7L));

        verify(memberBlockRepository).deleteAllInvolving(7L);
    }

    private Member member(Long id, String nickname) {
        Member member = new Member(new Nickname(nickname));
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}
