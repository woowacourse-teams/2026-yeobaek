package yeobaek.backend.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import yeobaek.backend.admin.dto.AdminDashboardMembersResponse;
import yeobaek.backend.admin.service.AdminMemberDashboardService;

@Tag(name = "관리자 대시보드")
@SecurityRequirement(name = "adminToken")
@RestController
@RequiredArgsConstructor
public class AdminMemberDashboardController {

    private final AdminMemberDashboardService adminMemberDashboardService;

    @Operation(summary = "회원별 모임 참여 현황 조회", description = "현재 회원별 참여 모임 수와 전체 평균, 분포를 조회한다.")
    @GetMapping("/api/admin/dashboard/members")
    public AdminDashboardMembersResponse findMembers() {
        return adminMemberDashboardService.findMembers();
    }
}
