package yeobaek.backend.appreciation.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import yeobaek.backend.application.appreciation.CommentModificationWorkflow;
import yeobaek.backend.application.appreciation.CommentQueryWorkflow;
import yeobaek.backend.application.appreciation.CommentReportWorkflow;
import yeobaek.backend.application.appreciation.CommentSharingWorkflow;
import yeobaek.backend.shared.identity.AppreciationId;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.support.LogCapture;
import yeobaek.backend.web.v1.CommentService;

class CommentLoggingTest {

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("댓글 신고 완료 로그는 신규 저장 여부와 회원·댓글 식별자를 기록한다")
    void logWhetherReportWasCreated(boolean alreadyReported) {
        var reportWorkflow = mock(CommentReportWorkflow.class);
        given(reportWorkflow.report(new MemberId(7L), new AppreciationId(11L))).willReturn(!alreadyReported);
        var service = new CommentService(mock(CommentSharingWorkflow.class), reportWorkflow,
                mock(CommentModificationWorkflow.class), mock(CommentQueryWorkflow.class),
                mock(yeobaek.backend.space.api.club.ClubApi.class),
                mock(yeobaek.backend.space.api.publicroom.PublicRoomApi.class),
                mock(yeobaek.backend.collaboration.api.binding.SpaceContentBindingApi.class),
                mock(yeobaek.backend.content.api.location.ContentLegacyLocationQueryApi.class),
                mock(yeobaek.backend.space.api.access.SpaceAccessApi.class));

        MDC.put("memberId", "7");
        try (var logs = new LogCapture("yeobaek.backend.appreciation.comment.service.CommentService")) {
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
