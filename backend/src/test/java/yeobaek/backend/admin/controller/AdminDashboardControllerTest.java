package yeobaek.backend.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
import yeobaek.backend.support.ControllerTest;

@WebMvcTest(AdminDashboardController.class)
@TestPropertySource(properties = "admin.token=controller-test-token")
class AdminDashboardControllerTest extends ControllerTest {

    @MockitoBean
    private AdminClubDashboardService adminClubDashboardService;

    @MockitoBean
    private AdminBookDashboardService adminBookDashboardService;

    @MockitoBean
    private AdminMemberDashboardService adminMemberDashboardService;

    @Test
    @DisplayName("모임 대시보드의 전체 응답 계약을 반환한다")
    void findClubsWithMemberAndCommentCounts() throws Exception {
        var response = new AdminDashboardClubsResponse(List.of(
                new AdminDashboardClubResponse(1L, "함께 읽기", 2L, "여백", BookStatus.DELETED, 3L, 4L)));
        given(adminClubDashboardService.findClubsWithMemberAndCommentCounts()).willReturn(response);

        mockMvc.perform(get("/api/admin/dashboard/clubs")
                        .header("X-Admin-Token", "controller-test-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.clubs").isArray())
                .andExpect(jsonPath("$.clubs.length()").value(1))
                .andExpect(jsonPath("$.clubs[0].clubId").value(1))
                .andExpect(jsonPath("$.clubs[0].name").value("함께 읽기"))
                .andExpect(jsonPath("$.clubs[0].bookId").value(2))
                .andExpect(jsonPath("$.clubs[0].bookTitle").value("여백"))
                .andExpect(jsonPath("$.clubs[0].bookStatus").value("DELETED"))
                .andExpect(jsonPath("$.clubs[0].memberCount").value(3))
                .andExpect(jsonPath("$.clubs[0].commentCount").value(4));

        verify(adminClubDashboardService, times(1)).findClubsWithMemberAndCommentCounts();
    }

    @Test
    @DisplayName("도서 대시보드의 전체 응답 계약을 반환한다")
    void findBooksWithClubCounts() throws Exception {
        var response = new AdminDashboardBooksResponse(List.of(
                new AdminDashboardBookResponse(2L, "여백", BookStatus.ACTIVE, 3L)));
        given(adminBookDashboardService.findBooksWithClubCounts()).willReturn(response);

        mockMvc.perform(get("/api/admin/dashboard/books")
                        .header("X-Admin-Token", "controller-test-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.books").isArray())
                .andExpect(jsonPath("$.books.length()").value(1))
                .andExpect(jsonPath("$.books[0].bookId").value(2))
                .andExpect(jsonPath("$.books[0].title").value("여백"))
                .andExpect(jsonPath("$.books[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.books[0].clubCount").value(3));

        verify(adminBookDashboardService, times(1)).findBooksWithClubCounts();
    }

    @Test
    @DisplayName("회원 대시보드의 전체 응답 계약을 반환한다")
    void findMemberClubParticipationStatistics() throws Exception {
        var response = new AdminDashboardMembersResponse(
                List.of(new AdminDashboardMemberResponse(7L, "독서가", 2L)),
                new BigDecimal("1.50"),
                List.of(
                        new AdminDashboardClubCountDistributionResponse(0L, 1L),
                        new AdminDashboardClubCountDistributionResponse(2L, 3L)));
        given(adminMemberDashboardService.findMemberClubParticipationStatistics()).willReturn(response);

        mockMvc.perform(get("/api/admin/dashboard/members")
                        .header("X-Admin-Token", "controller-test-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.members").isArray())
                .andExpect(jsonPath("$.members.length()").value(1))
                .andExpect(jsonPath("$.members[0].memberId").value(7))
                .andExpect(jsonPath("$.members[0].nickname").value("독서가"))
                .andExpect(jsonPath("$.members[0].clubCount").value(2))
                .andExpect(jsonPath("$.averageClubCount").value(1.5))
                .andExpect(jsonPath("$.distribution").isArray())
                .andExpect(jsonPath("$.distribution.length()").value(2))
                .andExpect(jsonPath("$.distribution[0].clubCount").value(0))
                .andExpect(jsonPath("$.distribution[0].memberCount").value(1))
                .andExpect(jsonPath("$.distribution[1].clubCount").value(2))
                .andExpect(jsonPath("$.distribution[1].memberCount").value(3));

        verify(adminMemberDashboardService, times(1)).findMemberClubParticipationStatistics();
    }

    @Test
    @DisplayName("각 통계 영역에 데이터가 없으면 빈 배열과 0.00 평균을 반환한다")
    void findDashboardStatisticsWhenEmpty() throws Exception {
        given(adminClubDashboardService.findClubsWithMemberAndCommentCounts())
                .willReturn(new AdminDashboardClubsResponse(List.of()));
        given(adminBookDashboardService.findBooksWithClubCounts())
                .willReturn(new AdminDashboardBooksResponse(List.of()));
        given(adminMemberDashboardService.findMemberClubParticipationStatistics())
                .willReturn(new AdminDashboardMembersResponse(
                        List.of(), new BigDecimal("0.00"), List.of()));

        mockMvc.perform(get("/api/admin/dashboard/clubs")
                        .header("X-Admin-Token", "controller-test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clubs.length()").value(0));
        mockMvc.perform(get("/api/admin/dashboard/books")
                        .header("X-Admin-Token", "controller-test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.books.length()").value(0));
        mockMvc.perform(get("/api/admin/dashboard/members")
                        .header("X-Admin-Token", "controller-test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members.length()").value(0))
                .andExpect(jsonPath("$.averageClubCount").value(0.0))
                .andExpect(jsonPath("$.distribution.length()").value(0));

        verify(adminClubDashboardService, times(1)).findClubsWithMemberAndCommentCounts();
        verify(adminBookDashboardService, times(1)).findBooksWithClubCounts();
        verify(adminMemberDashboardService, times(1)).findMemberClubParticipationStatistics();
    }

    @Test
    @DisplayName("관리자 토큰이 없으면 대시보드 조회를 거부한다")
    void rejectWithoutAdminToken() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/clubs"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(
                adminClubDashboardService,
                adminBookDashboardService,
                adminMemberDashboardService);
    }

    @Test
    @DisplayName("서비스 예외를 변경하지 않고 전파한다")
    void propagateServiceException() throws Exception {
        var serviceException = new IllegalStateException("대시보드 조회 실패");
        given(adminClubDashboardService.findClubsWithMemberAndCommentCounts()).willThrow(serviceException);

        var result = mockMvc.perform(get("/api/admin/dashboard/clubs")
                        .header("X-Admin-Token", "controller-test-token"))
                .andReturn();

        assertThat(result.getResolvedException()).isSameAs(serviceException);
        verify(adminClubDashboardService, times(1)).findClubsWithMemberAndCommentCounts();
    }
}
