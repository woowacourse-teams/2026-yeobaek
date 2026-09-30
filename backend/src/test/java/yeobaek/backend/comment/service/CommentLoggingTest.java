package yeobaek.backend.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.domain.vo.ContentSequence;
import yeobaek.backend.book.repository.PassageRepository;
import yeobaek.backend.book.repository.SentenceRepository;
import yeobaek.backend.club.domain.Club;
import yeobaek.backend.club.domain.ClubMember;
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

    @Test
    @DisplayName("새 댓글 수 로그는 현재 문단을 기록하고 관련 없는 댓글·문장 필드를 만들지 않는다")
    void logCurrentPassageWithoutIrrelevantFields() {
        var comments = mock(CommentRepository.class);
        var clubs = mock(ClubRepository.class);
        var memberships = mock(ClubMemberRepository.class);
        var passages = mock(PassageRepository.class);
        var club = mock(Club.class);
        var passage = mock(Passage.class);
        given(clubs.findById(5L)).willReturn(Optional.of(club));
        given(memberships.findJoinedByMemberIdAndClubId(7L, 5L)).willReturn(Optional.of(mock(ClubMember.class)));
        given(passages.findById(13L)).willReturn(Optional.of(passage));
        given(club.isReading(passage)).willReturn(true);
        given(passage.getSequence()).willReturn(new ContentSequence(4));
        given(comments.countNewVisibleCommentsWithinProgress(7L, 5L, 4)).willReturn(3L);
        var service = new CommentService(comments, mock(CommentReportRepository.class),
                mock(CommentViewRepository.class), clubs, memberships, mock(SentenceRepository.class),
                passages, mock(MemberRepository.class));

        MDC.put("memberId", "7");
        try (var logs = new LogCapture(CommentService.class.getName())) {
            service.countNewComments(7L, 5L, 13L);

            var event = logs.event("comment.countNewComments", "success");
            assertThat(event.getMDCPropertyMap()).containsEntry("memberId", "7");
            assertThat(List.of(logs.field(event, "clubId"), logs.field(event, "currentPassageId"),
                    logs.field(event, "resultCount"))).containsExactly(5L, 13L, 3L);
            assertThat(List.of(logs.hasField(event, "memberId"), logs.hasField(event, "sentenceId"),
                    logs.hasField(event, "commentId"))).containsOnly(false);
        } finally {
            MDC.remove("memberId");
        }
    }

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

        MDC.put("memberId", "7");
        try (var logs = new LogCapture(CommentService.class.getName())) {
            service.report(7L, 11L);

            var event = logs.event("comment.report", "success");
            assertThat(event.getMDCPropertyMap()).containsEntry("memberId", "7");
            assertThat(logs.hasField(event, "memberId")).isFalse();
            assertThat(logs.field(event, "commentId")).isEqualTo(11L);
            assertThat(logs.field(event, "reportCreated")).isEqualTo(!alreadyReported);
            assertThat(logs.hasField(event, "resultCount")).isFalse();
        } finally {
            MDC.remove("memberId");
        }
    }
}
