package yeobaek.backend.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.MemberBlock;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.dto.BlockedMemberResponse;
import yeobaek.backend.member.repository.MemberBlockRepository;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.support.BadRequestException;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.NotFoundException;

@ExtendWith(MockitoExtension.class)
class MemberBlockServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberBlockRepository memberBlockRepository;

    @InjectMocks
    private MemberBlockService memberBlockService;

    @Test
    @DisplayName("차단 목록에는 대상 회원의 현재 ID와 닉네임이 표시된다")
    void findBlockedMembers() {
        Member blocker = member(1L, "요청자");
        Member blocked = member(2L, "변경된닉네임");
        given(memberBlockRepository.findAllWithBlockedByBlockerId(1L))
                .willReturn(List.of(new MemberBlock(blocker, blocked)));

        var response = memberBlockService.findBlockedMembers(1L);

        assertThat(response.blockedMembers()).containsExactly(new BlockedMemberResponse(2L, "변경된닉네임"));
    }

    @Test
    @DisplayName("차단 요청의 요청자와 대상 회원을 구분해 저장한다")
    void block() {
        Member blocker = member(1L, "요청자");
        Member blocked = member(2L, "대상자");
        given(memberRepository.findById(2L)).willReturn(Optional.of(blocked));
        given(memberRepository.getReferenceById(1L)).willReturn(blocker);

        memberBlockService.block(1L, 2L);

        var saved = ArgumentCaptor.forClass(MemberBlock.class);
        verify(memberBlockRepository).save(saved.capture());
        assertThat(saved.getValue().getBlocker()).isSameAs(blocker);
        assertThat(saved.getValue().getBlocked()).isSameAs(blocked);
    }

    @Test
    @DisplayName("이미 차단한 회원의 재요청은 새 관계를 만들지 않는다")
    void repeatedBlock() {
        given(memberRepository.findById(2L)).willReturn(Optional.of(member(2L, "대상자")));
        given(memberBlockRepository.existsByBlockerIdAndBlockedId(1L, 2L)).willReturn(true);

        memberBlockService.block(1L, 2L);

        verify(memberBlockRepository, never()).save(any(MemberBlock.class));
    }

    @Test
    @DisplayName("본인 차단은 CANNOT_BLOCK_SELF로 거부한다")
    void rejectSelfBlock() {
        assertThatThrownBy(() -> memberBlockService.block(1L, 1L))
                .isInstanceOfSatisfying(BadRequestException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.CANNOT_BLOCK_SELF));
        verifyNoInteractions(memberRepository, memberBlockRepository);
    }

    @Test
    @DisplayName("존재하지 않는 대상은 차단할 수 없다")
    void rejectUnknownBlockTarget() {
        assertThatThrownBy(() -> memberBlockService.block(1L, 2L))
                .isInstanceOfSatisfying(NotFoundException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND));
        verifyNoInteractions(memberBlockRepository);
    }

    @Test
    @DisplayName("차단 해제는 요청자와 대상 사이의 관계만 제거한다")
    void unblock() {
        given(memberRepository.existsById(2L)).willReturn(true);

        memberBlockService.unblock(1L, 2L);

        verify(memberBlockRepository).deleteByBlockerIdAndBlockedId(1L, 2L);
    }

    @Test
    @DisplayName("삭제된 대상의 차단 해제는 MEMBER_NOT_FOUND로 거부한다")
    void rejectUnknownUnblockTarget() {
        assertThatThrownBy(() -> memberBlockService.unblock(1L, 2L))
                .isInstanceOfSatisfying(NotFoundException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND));
        verifyNoInteractions(memberBlockRepository);
    }

    private Member member(Long memberId, String nickname) {
        Member member = new Member(new Nickname(nickname));
        ReflectionTestUtils.setField(member, "id", memberId);
        return member;
    }
}
