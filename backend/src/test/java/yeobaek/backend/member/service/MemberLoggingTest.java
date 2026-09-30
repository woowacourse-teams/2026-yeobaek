package yeobaek.backend.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.comment.repository.CommentRepository;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.publicroom.repository.PublicRoomActivityRepository;
import yeobaek.backend.support.LogCapture;

class MemberLoggingTest {

    @Test
    @DisplayName("회원 생성 로그는 인증 요청자가 아닌 새 회원 식별자로 기록한다")
    void logCreatedMemberId() {
        var members = mock(MemberRepository.class);
        var savedMember = mock(Member.class);
        given(savedMember.getId()).willReturn(7L);
        given(savedMember.getNickname()).willReturn("새회원");
        given(members.save(any(Member.class))).willReturn(savedMember);
        var service = new MemberService(members, mock(CommentRepository.class), mock(ClubMemberRepository.class),
                mock(PublicRoomActivityRepository.class));

        try (var logs = new LogCapture(MemberService.class.getName())) {
            service.create(new Nickname("새회원"));

            var event = logs.event("member.create", "success");
            assertThat(logs.field(event, "createdMemberId")).isEqualTo(7L);
            assertThat(logs.hasField(event, "memberId")).isFalse();
        }
    }

    @Test
    @DisplayName("영속성 실패 전 시작 로그만 남고 성공 로그는 남지 않는다")
    void failedPersistenceLeavesAttemptWithoutFalseSuccess() {
        var members = mock(MemberRepository.class);
        var comments = mock(CommentRepository.class);
        var clubs = mock(ClubMemberRepository.class);
        var service = new MemberService(members, comments, clubs, mock(PublicRoomActivityRepository.class));
        var failure = new IllegalStateException("database unavailable");
        doThrow(failure).when(comments).deleteAllByMemberId(7L);

        MDC.put("memberId", "7");
        try (var logs = new LogCapture(MemberService.class.getName())) {
            assertThatThrownBy(() -> service.delete(7L)).isSameAs(failure);
            assertThat(logs.events("member.delete")).isNotEmpty();
            assertThat(logs.events("member.deleteComments")).isNotEmpty();
            assertThat(logs.events()).allSatisfy(event -> {
                assertThat(event.getMDCPropertyMap()).containsEntry("memberId", "7");
                assertThat(logs.hasField(event, "memberId")).isFalse();
            });
            assertThat(logs.events()).extracting(event -> logs.field(event, "result"))
                    .doesNotContain("success");
        } finally {
            MDC.remove("memberId");
        }
    }
}
