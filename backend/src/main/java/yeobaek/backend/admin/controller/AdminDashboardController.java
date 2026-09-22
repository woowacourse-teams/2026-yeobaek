package yeobaek.backend.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import yeobaek.backend.admin.dto.AdminDashboardBooksResponse;
import yeobaek.backend.admin.dto.AdminDashboardClubsResponse;
import yeobaek.backend.admin.dto.AdminDashboardMembersResponse;
import yeobaek.backend.admin.service.AdminBookDashboardService;
import yeobaek.backend.admin.service.AdminClubDashboardService;
import yeobaek.backend.admin.service.AdminMemberDashboardService;

@Tag(name = "관리자 대시보드")
@SecurityRequirement(name = "adminToken")
@RestController
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminClubDashboardService adminClubDashboardService;
    private final AdminBookDashboardService adminBookDashboardService;
    private final AdminMemberDashboardService adminMemberDashboardService;

    @Operation(summary = "모임별 현황 조회", description = "모임, 도서 상태, 현재 참여자 수와 전체 댓글 수를 조회한다.")
    @GetMapping("/api/admin/dashboard/clubs")
    public AdminDashboardClubsResponse findClubsWithMemberAndCommentCounts() {
        return adminClubDashboardService.findClubsWithMemberAndCommentCounts();
    }

    @Operation(summary = "도서별 모임 현황 조회", description = "삭제된 도서와 모임이 없는 도서를 포함해 모임 수를 조회한다.")
    @GetMapping("/api/admin/dashboard/books")
    public AdminDashboardBooksResponse findBooksWithClubCounts() {
        return adminBookDashboardService.findBooksWithClubCounts();
    }

    @Operation(summary = "회원별 모임 참여 현황 조회", description = "현재 회원별 참여 모임 수와 전체 평균, 분포를 조회한다.")
    @GetMapping("/api/admin/dashboard/members")
    public AdminDashboardMembersResponse findMemberClubParticipationStatistics() {
        return adminMemberDashboardService.findMemberClubParticipationStatistics();
    }
}
