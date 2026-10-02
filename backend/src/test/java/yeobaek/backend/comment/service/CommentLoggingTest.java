package yeobaek.backend.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import yeobaek.backend.application.appreciation.CommentModificationWorkflow;
import yeobaek.backend.application.appreciation.CommentQueryWorkflow;
import yeobaek.backend.application.appreciation.CommentReportWorkflow;
import yeobaek.backend.application.appreciation.CommentSharingWorkflow;
import yeobaek.backend.foundation.identity.AppreciationId;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.support.LogCapture;
import yeobaek.backend.web.compatibility.CommentService;

class CommentLoggingTest {

    @Test
    @DisplayName("새 댓글 수 로그는 현재 문단을 기록하고 관련 없는 댓글·문장 필드를 만들지 않는다")
    void logCurrentPassageWithoutIrrelevantFields() {
        var queries = mock(CommentQueryWorkflow.class);
        given(queries.countNewClubComments(new MemberId(7L), 5L, 13L)).willReturn(3L);
        var service = new CommentService(mock(CommentSharingWorkflow.class), mock(CommentReportWorkflow.class),
                mock(CommentModificationWorkflow.class), queries);

        MDC.put("memberId", "7");
        try (var logs = new LogCapture("yeobaek.backend.comment.service.CommentService")) {
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
        var reportWorkflow = mock(CommentReportWorkflow.class);
        given(reportWorkflow.report(new MemberId(7L), new AppreciationId(11L))).willReturn(!alreadyReported);
        var service = new CommentService(mock(CommentSharingWorkflow.class), reportWorkflow,
                mock(CommentModificationWorkflow.class), mock(CommentQueryWorkflow.class));

        MDC.put("memberId", "7");
        try (var logs = new LogCapture("yeobaek.backend.comment.service.CommentService")) {
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
