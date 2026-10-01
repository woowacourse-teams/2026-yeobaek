package yeobaek.backend.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import yeobaek.backend.admin.dto.AdminDashboardClubCountDistributionResponse;
import yeobaek.backend.admin.dto.AdminDashboardMemberResponse;
import yeobaek.backend.admin.dto.AdminDashboardMembersResponse;
import yeobaek.backend.admin.service.AdminBookDashboardService;
import yeobaek.backend.admin.service.AdminClubDashboardService;
import yeobaek.backend.admin.service.AdminMemberDashboardService;
import yeobaek.backend.support.LogCapture;

class AdminDashboardLoggingTest {

    private final AdminClubDashboardService clubService = mock(AdminClubDashboardService.class);
    private final AdminBookDashboardService bookService = mock(AdminBookDashboardService.class);
    private final AdminMemberDashboardService memberService = mock(AdminMemberDashboardService.class);
    private final AdminDashboardController controller =
            new AdminDashboardController(clubService, bookService, memberService);

    @Test
    @DisplayName("회원 대시보드 완료 로그는 회원 ID와 참여 수를 포함하고 닉네임을 제외한다")
    void logMemberDashboardPolicy() {
        given(memberService.findMemberClubParticipationStatistics()).willReturn(new AdminDashboardMembersResponse(
                List.of(new AdminDashboardMemberResponse(7L, "비공개 닉네임", 2L)),
                new BigDecimal("1.50"),
                List.of(new AdminDashboardClubCountDistributionResponse(2L, 3L))));

        try (var logs = new LogCapture(AdminDashboardController.class.getName())) {
            controller.findMemberClubParticipationStatistics();

            var event = logs.event("admin.dashboard.findMemberClubParticipationStatistics", "success");
            var members = (List<?>) logs.field(event, "members");
            var summary = (Map<?, ?>) members.getFirst();
            assertThat(members).isNotEmpty();
            assertThat(summary.get("memberId")).isEqualTo(7L);
            assertThat(summary.get("clubCount")).isEqualTo(2L);
            assertThat(summary.containsKey("nickname")).isFalse();
            assertThat(logs.structuredText()).doesNotContain("비공개 닉네임");
        }
    }
}
