package yeobaek.backend.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

import java.sql.SQLException;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
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
        given(memberRepository.saveAndFlush(any(Member.class))).willReturn(saved);

        MemberCreateResponse response = memberService.create(new Nickname("민서"));

        assertThat(response.memberId()).isEqualTo(1L);
        assertThat(response.nickname()).isEqualTo("민서");
    }

    @ParameterizedTest
    @ValueSource(strings = {"uk_members_nickname", "members.uk_members_nickname"})
    @DisplayName("회원 생성 중 닉네임 제약 위반을 원인 예외를 보존한 업무 예외로 변환한다")
    void rejectDuplicateNicknameOnCreate(String constraintName) {
        var failure = constraintViolation(constraintName);
        given(memberRepository.saveAndFlush(any(Member.class))).willThrow(failure);

        assertThatThrownBy(() -> memberService.create(new Nickname("민서")))
                .isInstanceOfSatisfying(InvalidRequestException.class, exception -> {
                    assertThat(exception.getMessage()).isEqualTo("이미 사용 중인 닉네임입니다.");
                    assertThat(exception.getCause()).isSameAs(failure);
                    assertThat(exception.getLogContext()).containsEntry("reason", "duplicate_nickname");
                });
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
        verify(memberRepository).flush();
    }

    @ParameterizedTest
    @ValueSource(strings = {"uk_members_nickname", "members.uk_members_nickname"})
    @DisplayName("닉네임 변경 중 닉네임 제약 위반을 원인 예외를 보존한 업무 예외로 변환한다")
    void rejectDuplicateNicknameOnUpdate(String constraintName) {
        Member member = new Member(new Nickname("기존닉네임"));
        given(memberRepository.findById(7L)).willReturn(Optional.of(member));
        var failure = constraintViolation(constraintName);
        doThrow(failure).when(memberRepository).flush();

        assertThatThrownBy(() -> memberService.updateNickname(7L, new Nickname("중복닉네임")))
                .isInstanceOfSatisfying(InvalidRequestException.class, exception -> {
                    assertThat(exception.getMessage()).isEqualTo("이미 사용 중인 닉네임입니다.");
                    assertThat(exception.getCause()).isSameAs(failure);
                    assertThat(exception.getLogContext()).containsEntry("reason", "duplicate_nickname");
                });
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"fk_other", "uk_other"})
    @DisplayName("회원 생성 중 다른 제약 위반은 변환하지 않고 그대로 전파한다")
    void propagateOtherCreateConstraintViolation(String constraintName) {
        var failure = constraintViolation(constraintName);
        given(memberRepository.saveAndFlush(any(Member.class))).willThrow(failure);

        assertThatThrownBy(() -> memberService.create(new Nickname("민서"))).isSameAs(failure);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"fk_other", "uk_other"})
    @DisplayName("닉네임 변경 중 다른 제약 위반은 변환하지 않고 그대로 전파한다")
    void propagateOtherUpdateConstraintViolation(String constraintName) {
        given(memberRepository.findById(7L)).willReturn(Optional.of(new Member(new Nickname("기존닉네임"))));
        var failure = constraintViolation(constraintName);
        doThrow(failure).when(memberRepository).flush();

        assertThatThrownBy(() -> memberService.updateNickname(7L, new Nickname("새닉네임"))).isSameAs(failure);
    }

    @Test
    @DisplayName("회원 생성 중 원인 없는 무결성 오류는 그대로 전파한다")
    void propagateCreateIntegrityViolationWithoutCause() {
        var failure = new DataIntegrityViolationException("다른 무결성 오류");
        given(memberRepository.saveAndFlush(any(Member.class))).willThrow(failure);

        assertThatThrownBy(() -> memberService.create(new Nickname("민서"))).isSameAs(failure);
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

    private DataIntegrityViolationException constraintViolation(String constraintName) {
        var violation = new ConstraintViolationException("제약 위반", new SQLException("제약 위반"), constraintName);
        return new DataIntegrityViolationException("저장 실패", new RuntimeException("중간 원인", violation));
    }
}
