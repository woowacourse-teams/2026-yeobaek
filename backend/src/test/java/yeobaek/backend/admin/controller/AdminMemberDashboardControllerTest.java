package yeobaek.backend.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import yeobaek.backend.admin.dto.AdminDashboardClubCountDistributionResponse;
import yeobaek.backend.admin.dto.AdminDashboardMemberResponse;
import yeobaek.backend.admin.dto.AdminDashboardMembersResponse;
import yeobaek.backend.admin.service.AdminMemberDashboardService;
import yeobaek.backend.support.ControllerTest;

@WebMvcTest(AdminMemberDashboardController.class)
@TestPropertySource(properties = "admin.token=controller-test-token")
class AdminMemberDashboardControllerTest extends ControllerTest {

    @MockitoBean
    private AdminMemberDashboardService adminMemberDashboardService;

    @Test
    @DisplayName("회원 대시보드의 전체 응답 계약을 반환한다")
    void findMembers() throws Exception {
        var response = new AdminDashboardMembersResponse(
                List.of(new AdminDashboardMemberResponse(7L, "독서가", 2L)),
                new BigDecimal("1.50"),
                List.of(
                        new AdminDashboardClubCountDistributionResponse(0L, 1L),
                        new AdminDashboardClubCountDistributionResponse(2L, 3L)));
        given(adminMemberDashboardService.findMembers()).willReturn(response);

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

        verify(adminMemberDashboardService, times(1)).findMembers();
    }

    @Test
    @DisplayName("회원이 없으면 빈 배열과 0.00 평균을 반환한다")
    void findMembersWhenEmpty() throws Exception {
        given(adminMemberDashboardService.findMembers()).willReturn(new AdminDashboardMembersResponse(
                List.of(), new BigDecimal("0.00"), List.of()));

        mockMvc.perform(get("/api/admin/dashboard/members")
                        .header("X-Admin-Token", "controller-test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members").isArray())
                .andExpect(jsonPath("$.members.length()").value(0))
                .andExpect(jsonPath("$.averageClubCount").value(0.0))
                .andExpect(jsonPath("$.distribution").isArray())
                .andExpect(jsonPath("$.distribution.length()").value(0));

        verify(adminMemberDashboardService, times(1)).findMembers();
    }

    @Test
    @DisplayName("서비스 예외를 변경하지 않고 전파한다")
    void propagateServiceException() throws Exception {
        var serviceException = new IllegalStateException("회원 통계 조회 실패");
        given(adminMemberDashboardService.findMembers()).willThrow(serviceException);

        var result = mockMvc.perform(get("/api/admin/dashboard/members")
                        .header("X-Admin-Token", "controller-test-token"))
                .andReturn();

        assertThat(result.getResolvedException()).isSameAs(serviceException);
        verify(adminMemberDashboardService, times(1)).findMembers();
    }
}
