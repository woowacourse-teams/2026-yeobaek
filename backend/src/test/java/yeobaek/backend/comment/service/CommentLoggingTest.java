package yeobaek.backend.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import yeobaek.backend.book.repository.PassageRepository;
import yeobaek.backend.book.repository.SentenceRepository;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.club.repository.ClubRepository;
import yeobaek.backend.comment.domain.Comment;
import yeobaek.backend.comment.repository.CommentReportRepository;
import yeobaek.backend.comment.repository.CommentRepository;
import yeobaek.backend.comment.repository.CommentViewRepository;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.support.LogCapture;

class CommentLoggingTest {

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("댓글 신고 완료 로그는 신규 저장 여부와 회원·댓글 식별자를 기록한다")
    void logWhetherReportWasCreated(boolean alreadyReported) {
        var comments = mock(CommentRepository.class);
        var reports = mock(CommentReportRepository.class);
        var memberships = mock(ClubMemberRepository.class);
        var members = mock(MemberRepository.class);
        var comment = mock(Comment.class);
        given(comments.findVisibleWithContextById(7L, 11L)).willReturn(Optional.of(comment));
        given(memberships.existsJoinedByMemberIdAndCommentId(7L, 11L)).willReturn(true);
        given(reports.existsByReporterIdAndCommentId(7L, 11L)).willReturn(alreadyReported);
        given(members.getReferenceById(7L)).willReturn(mock(Member.class));
        var service = new CommentService(comments, reports, mock(CommentViewRepository.class),
                mock(ClubRepository.class), memberships, mock(SentenceRepository.class),
                mock(PassageRepository.class), members);

        try (var logs = new LogCapture(CommentService.class.getName())) {
            service.report(7L, 11L);

            var event = logs.event("comment.report", "success");
            assertThat(logs.field(event, "memberId")).isEqualTo(7L);
            assertThat(logs.field(event, "commentId")).isEqualTo(11L);
            assertThat(logs.field(event, "reportCreated")).isEqualTo(!alreadyReported);
            assertThat(logs.hasField(event, "resultCount")).isFalse();
        }
    }
}
