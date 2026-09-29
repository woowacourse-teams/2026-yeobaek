package yeobaek.backend.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

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
            assertThat(logs.events()).extracting(event -> logs.field(event, "operation"))
                    .containsExactly("member.delete", "member.deleteComments");
            assertThat(logs.events()).allSatisfy(event -> assertThat(logs.field(event, "result")).isNull());
            assertThat(logs.events()).allSatisfy(event -> assertThat(event.getThrowableProxy()).isNull());
            verifyNoInteractions(members, clubs);
        }
    }
}
