package yeobaek.backend.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import yeobaek.backend.admin.dto.AdminDashboardClubsResponse;
import yeobaek.backend.admin.service.AdminClubDashboardService;

@Tag(name = "관리자 대시보드")
@SecurityRequirement(name = "adminToken")
@RestController
@RequiredArgsConstructor
public class AdminClubDashboardController {

    private final AdminClubDashboardService adminClubDashboardService;

    @Operation(summary = "모임별 현황 조회", description = "모임, 도서 상태, 현재 참여자 수와 전체 댓글 수를 조회한다.")
    @GetMapping("/api/admin/dashboard/clubs")
    public AdminDashboardClubsResponse findClubs() {
        return adminClubDashboardService.findClubs();
    }
}
