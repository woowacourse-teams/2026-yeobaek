package yeobaek.backend.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.comment.repository.CommentRepository;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.dto.MemberCreateResponse;
import yeobaek.backend.member.dto.MemberNicknameUpdateResponse;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.publicroom.repository.PublicRoomMemberRepository;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.InvalidRequestException;
import yeobaek.backend.support.NotFoundException;

@ExtendWith(MockitoExtension.class)
class MemberServiceUnitTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ClubMemberRepository clubMemberRepository;

    @Mock
    private PublicRoomMemberRepository publicRoomMemberRepository;

    @InjectMocks
    private MemberService memberService;

    @Test
    @DisplayName("회원을 생성하면 발급된 ID와 닉네임을 반환한다")
    void create() {
        Member saved = new Member(new Nickname("민서"));
        ReflectionTestUtils.setField(saved, "id", 1L);
        given(memberRepository.existsByNickname("민서")).willReturn(false);
        given(memberRepository.save(any(Member.class))).willReturn(saved);

        MemberCreateResponse response = memberService.create(new Nickname("민서"));

        assertThat(response.memberId()).isEqualTo(1L);
        assertThat(response.nickname()).isEqualTo("민서");
    }

    @Test
    @DisplayName("이미 사용 중인 닉네임이면 회원을 저장하지 않는다")
    void duplicateNicknameNotSaved() {
        given(memberRepository.existsByNickname("민서")).willReturn(true);

        assertThatThrownBy(() -> memberService.create(new Nickname("민서")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 사용 중인 닉네임입니다.");

        verify(memberRepository, never()).save(any());
    }

    @Test
    @DisplayName("ID로 조회한 회원의 닉네임을 변경하고 응답한다")
    void updateNickname() {
        Member member = new Member(new Nickname("기존닉네임"));
        ReflectionTestUtils.setField(member, "id", 7L);
        given(memberRepository.findById(7L)).willReturn(Optional.of(member));

        MemberNicknameUpdateResponse response = memberService.updateNickname(7L, new Nickname("새닉네임"));

        assertThat(response).isEqualTo(new MemberNicknameUpdateResponse(7L, "새닉네임"));
        assertThat(member.getNickname()).isEqualTo("새닉네임");
        verify(memberRepository).existsByNicknameExcludingMember("새닉네임", 7L);
    }

    @Test
    @DisplayName("다른 회원과 중복되는 닉네임으로 변경하지 않는다")
    void rejectDuplicateNicknameOnUpdate() {
        Member member = new Member(new Nickname("기존닉네임"));
        given(memberRepository.findById(7L)).willReturn(Optional.of(member));
        given(memberRepository.existsByNicknameExcludingMember("중복닉네임", 7L)).willReturn(true);

        assertThatThrownBy(() -> memberService.updateNickname(7L, new Nickname("중복닉네임")))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("이미 사용 중인 닉네임입니다.");
        assertThat(member.getNickname()).isEqualTo("기존닉네임");
    }

    @Test
    @DisplayName("존재하지 않는 회원의 닉네임을 변경하면 예외가 발생한다")
    void rejectMissingMemberOnUpdate() {
        given(memberRepository.findById(7L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.updateNickname(7L, new Nickname("새닉네임")))
                .isInstanceOfSatisfying(NotFoundException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND));
    }

    @Test
    @DisplayName("계정을 삭제할 때 댓글, 공개방 활동, 모임 참여 기록, 회원 순으로 삭제한다")
    void delete() {
        memberService.delete(1L);

        InOrder ordered = inOrder(commentRepository, publicRoomMemberRepository,
                clubMemberRepository, memberRepository);
        ordered.verify(commentRepository).deleteAllByMemberId(1L);
        ordered.verify(publicRoomMemberRepository).deleteAllByMemberId(1L);
        ordered.verify(clubMemberRepository).deleteAllByMemberId(1L);
        ordered.verify(memberRepository).deleteById(1L);
    }
}
