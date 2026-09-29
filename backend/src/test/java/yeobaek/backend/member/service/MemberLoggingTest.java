package yeobaek.backend.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.comment.repository.CommentRepository;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.support.LogCapture;

class MemberLoggingTest {

    @Test
    @DisplayName("영속성 실패 전 시작 로그만 남고 성공 로그는 남지 않는다")
    void failedPersistenceLeavesAttemptWithoutFalseSuccess() {
        var members = mock(MemberRepository.class);
        var comments = mock(CommentRepository.class);
        var clubs = mock(ClubMemberRepository.class);
        var service = new MemberService(members, comments, clubs);
        var failure = new IllegalStateException("database unavailable");
        doThrow(failure).when(comments).deleteAllByMemberId(7L);

        try (var logs = new LogCapture(MemberService.class.getName())) {
            assertThatThrownBy(() -> service.delete(7L)).isSameAs(failure);
            assertThat(logs.events("member.delete")).isNotEmpty();
            assertThat(logs.events("member.deleteComments")).isNotEmpty();
            assertThat(logs.events()).allSatisfy(event -> assertThat(logs.field(event, "memberId")).isEqualTo(7L));
            assertThat(logs.events()).extracting(event -> logs.field(event, "result"))
                    .doesNotContain("success");
        }
    }
}
