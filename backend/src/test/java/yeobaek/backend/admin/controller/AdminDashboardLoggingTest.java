package yeobaek.backend.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import yeobaek.backend.admin.dto.AdminDashboardBookResponse;
import yeobaek.backend.admin.dto.AdminDashboardBooksResponse;
import yeobaek.backend.admin.dto.AdminDashboardClubCountDistributionResponse;
import yeobaek.backend.admin.dto.AdminDashboardClubResponse;
import yeobaek.backend.admin.dto.AdminDashboardClubsResponse;
import yeobaek.backend.admin.dto.AdminDashboardMemberResponse;
import yeobaek.backend.admin.dto.AdminDashboardMembersResponse;
import yeobaek.backend.admin.service.AdminBookDashboardService;
import yeobaek.backend.admin.service.AdminClubDashboardService;
import yeobaek.backend.admin.service.AdminMemberDashboardService;
import yeobaek.backend.book.domain.BookStatus;
import yeobaek.backend.support.LogCapture;

class AdminDashboardLoggingTest {

    private final AdminClubDashboardService clubService = mock(AdminClubDashboardService.class);
    private final AdminBookDashboardService bookService = mock(AdminBookDashboardService.class);
    private final AdminMemberDashboardService memberService = mock(AdminMemberDashboardService.class);
    private final AdminDashboardController controller =
            new AdminDashboardController(clubService, bookService, memberService);

    @Test
    @DisplayName("모임 대시보드 완료 로그에는 식별자·상태·집계만 기록한다")
    void logClubDashboardSummary() {
        given(clubService.findClubsWithMemberAndCommentCounts()).willReturn(new AdminDashboardClubsResponse(List.of(
                new AdminDashboardClubResponse(1L, "비공개 모임명", 2L, "비공개 도서명",
                        BookStatus.ACTIVE, 3L, 4L))));

        try (var logs = new LogCapture(AdminDashboardController.class.getName())) {
            controller.findClubsWithMemberAndCommentCounts();

            Object clubs = logs.field(logs.events().get(1), "clubs");
            assertThat(clubs).isEqualTo(List.of(Map.of(
                    "clubId", 1L, "bookId", 2L, "bookStatus", BookStatus.ACTIVE,
                    "memberCount", 3L, "commentCount", 4L)));
            assertThat(logs.structuredText()).doesNotContain("비공개 모임명", "비공개 도서명");
        }
    }

    @Test
    @DisplayName("도서 대시보드 완료 로그에는 도서 ID·상태·모임 수만 기록한다")
    void logBookDashboardSummary() {
        given(bookService.findBooksWithClubCounts()).willReturn(new AdminDashboardBooksResponse(List.of(
                new AdminDashboardBookResponse(2L, "비공개 도서명", BookStatus.DELETED, 3L))));

        try (var logs = new LogCapture(AdminDashboardController.class.getName())) {
            controller.findBooksWithClubCounts();

            Object books = logs.field(logs.events().get(1), "books");
            assertThat(books).isEqualTo(List.of(Map.of(
                    "bookId", 2L, "bookStatus", BookStatus.DELETED, "clubCount", 3L)));
            assertThat(logs.structuredText()).doesNotContain("비공개 도서명");
        }
    }

    @Test
    @DisplayName("회원 대시보드 완료 로그에는 회원 ID와 참여 통계만 기록한다")
    void logMemberDashboardSummary() {
        given(memberService.findMemberClubParticipationStatistics()).willReturn(new AdminDashboardMembersResponse(
                List.of(new AdminDashboardMemberResponse(7L, "비공개 닉네임", 2L)),
                new BigDecimal("1.50"),
                List.of(new AdminDashboardClubCountDistributionResponse(2L, 3L))));

        try (var logs = new LogCapture(AdminDashboardController.class.getName())) {
            controller.findMemberClubParticipationStatistics();

            var event = logs.events().get(1);
            assertThat(logs.field(event, "members"))
                    .isEqualTo(List.of(Map.of("memberId", 7L, "clubCount", 2L)));
            assertThat(logs.field(event, "averageClubCount")).isEqualTo(new BigDecimal("1.50"));
            assertThat(logs.field(event, "distribution"))
                    .isEqualTo(List.of(Map.of("clubCount", 2L, "memberCount", 3L)));
            assertThat(logs.structuredText()).doesNotContain("비공개 닉네임");
        }
    }
}
