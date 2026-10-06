package yeobaek.backend.web.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import yeobaek.backend.application.account.AccountDeletionWorkflow;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.member.service.MemberService;
import yeobaek.backend.support.LogCapture;

class MemberServiceTest {

    @Test
    void delegatesAccountDeletionToApplicationWorkflow() {
        var workflow = mock(AccountDeletionWorkflow.class);
        var service = new yeobaek.backend.web.v1.MemberService(
                mock(MemberService.class), workflow);

        service.delete(7L);

        verify(workflow).delete(new MemberId(7L));
    }

    @Test
    void failedPersistenceLeavesAttemptWithoutFalseSuccess() {
        var workflow = mock(AccountDeletionWorkflow.class);
        var service = new yeobaek.backend.web.v1.MemberService(
                mock(MemberService.class), workflow);
        var failure = new IllegalStateException("database unavailable");
        doThrow(failure).when(workflow).delete(new MemberId(7L));

        MDC.put("memberId", "7");
        try (var logs = new LogCapture("yeobaek.backend.member.service.MemberService")) {
            assertThatThrownBy(() -> service.delete(7L)).isSameAs(failure);
            assertThat(logs.events("member.delete")).isNotEmpty();
            assertThat(logs.events("member.deleteData")).isNotEmpty();
            assertThat(logs.events()).extracting(event -> logs.field(event, "result"))
                    .doesNotContain("success");
        } finally {
            MDC.remove("memberId");
        }
    }
}
