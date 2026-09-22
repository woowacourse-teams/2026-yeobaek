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

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import yeobaek.backend.admin.dto.AdminDashboardClubResponse;
import yeobaek.backend.admin.dto.AdminDashboardClubsResponse;
import yeobaek.backend.admin.service.AdminClubDashboardService;
import yeobaek.backend.book.domain.BookStatus;
import yeobaek.backend.support.ControllerTest;

@WebMvcTest(AdminClubDashboardController.class)
@TestPropertySource(properties = "admin.token=controller-test-token")
class AdminClubDashboardControllerTest extends ControllerTest {

    @MockitoBean
    private AdminClubDashboardService adminClubDashboardService;

    @Test
    @DisplayName("모임 대시보드의 전체 응답 계약을 반환한다")
    void findClubs() throws Exception {
        var response = new AdminDashboardClubsResponse(List.of(
                new AdminDashboardClubResponse(1L, "함께 읽기", 2L, "여백", BookStatus.DELETED, 3L, 4L)));
        given(adminClubDashboardService.findClubs()).willReturn(response);

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

        verify(adminClubDashboardService, times(1)).findClubs();
    }

    @Test
    @DisplayName("관리자 토큰이 없으면 대시보드 조회를 거부한다")
    void rejectWithoutAdminToken() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/clubs"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(adminClubDashboardService);
    }

    @Test
    @DisplayName("모임이 없으면 빈 배열을 반환한다")
    void findClubsWhenEmpty() throws Exception {
        given(adminClubDashboardService.findClubs()).willReturn(new AdminDashboardClubsResponse(List.of()));

        mockMvc.perform(get("/api/admin/dashboard/clubs")
                        .header("X-Admin-Token", "controller-test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clubs").isArray())
                .andExpect(jsonPath("$.clubs.length()").value(0));

        verify(adminClubDashboardService, times(1)).findClubs();
    }

    @Test
    @DisplayName("서비스 예외를 변경하지 않고 전파한다")
    void propagateServiceException() throws Exception {
        var serviceException = new IllegalStateException("모임 통계 조회 실패");
        given(adminClubDashboardService.findClubs()).willThrow(serviceException);

        var result = mockMvc.perform(get("/api/admin/dashboard/clubs")
                        .header("X-Admin-Token", "controller-test-token"))
                .andReturn();

        assertThat(result.getResolvedException()).isSameAs(serviceException);
        verify(adminClubDashboardService, times(1)).findClubs();
    }
}
