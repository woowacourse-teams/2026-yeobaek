package yeobaek.backend.admin.controller;

import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.PHASE;
import static yeobaek.backend.support.LogField.SUCCESS;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class AdminDashboardController {

    private final AdminClubDashboardService adminClubDashboardService;
    private final AdminBookDashboardService adminBookDashboardService;
    private final AdminMemberDashboardService adminMemberDashboardService;

    @Operation(summary = "모임별 현황 조회", description = "모임, 도서 상태, 현재 참여자 수와 전체 댓글 수를 조회한다.")
    @GetMapping("/api/admin/dashboard/clubs")
    public AdminDashboardClubsResponse findClubsWithMemberAndCommentCounts() {
        log.atInfo().addKeyValue(OPERATION, "admin.dashboard.findClubsWithMemberAndCommentCounts")
                .log("모임 현황 API 처리를 시작합니다.");
        AdminDashboardClubsResponse response = adminClubDashboardService.findClubsWithMemberAndCommentCounts();
        log.atInfo().addKeyValue(OPERATION, "admin.dashboard.findClubsWithMemberAndCommentCounts").addKeyValue(PHASE, SUCCESS)
                .addKeyValue("resultCount", response.clubs().size())
                .addKeyValue("clubs", response.clubs().stream()
                        .map(club -> Map.of(
                                "clubId", club.clubId(),
                                "bookId", club.bookId(),
                                "bookStatus", club.bookStatus(),
                                "memberCount", club.memberCount(),
                                "commentCount", club.commentCount()))
                        .toList())
                .log("모임 현황 API 처리를 완료했습니다.");
        return response;
    }

    @Operation(summary = "도서별 모임 현황 조회", description = "삭제된 도서와 모임이 없는 도서를 포함해 모임 수를 조회한다.")
    @GetMapping("/api/admin/dashboard/books")
    public AdminDashboardBooksResponse findBooksWithClubCounts() {
        log.atInfo().addKeyValue(OPERATION, "admin.dashboard.findBooksWithClubCounts")
                .log("도서 현황 API 처리를 시작합니다.");
        AdminDashboardBooksResponse response = adminBookDashboardService.findBooksWithClubCounts();
        log.atInfo().addKeyValue(OPERATION, "admin.dashboard.findBooksWithClubCounts").addKeyValue(PHASE, SUCCESS)
                .addKeyValue("resultCount", response.books().size())
                .addKeyValue("books", response.books().stream()
                        .map(book -> Map.of(
                                "bookId", book.bookId(),
                                "bookStatus", book.status(),
                                "clubCount", book.clubCount()))
                        .toList())
                .log("도서 현황 API 처리를 완료했습니다.");
        return response;
    }

    @Operation(summary = "회원별 모임 참여 현황 조회", description = "현재 회원별 참여 모임 수와 전체 평균, 분포를 조회한다.")
    @GetMapping("/api/admin/dashboard/members")
    public AdminDashboardMembersResponse findMemberClubParticipationStatistics() {
        log.atInfo().addKeyValue(OPERATION, "admin.dashboard.findMemberClubParticipationStatistics")
                .log("회원 현황 API 처리를 시작합니다.");
        AdminDashboardMembersResponse response = adminMemberDashboardService.findMemberClubParticipationStatistics();
        log.atInfo().addKeyValue(OPERATION, "admin.dashboard.findMemberClubParticipationStatistics").addKeyValue(PHASE, SUCCESS)
                .addKeyValue("memberCount", response.members().size())
                .addKeyValue("averageClubCount", response.averageClubCount())
                .addKeyValue("members", response.members().stream()
                        .map(member -> Map.of(
                                "memberId", member.memberId(),
                                "clubCount", member.clubCount()))
                        .toList())
                .addKeyValue("distribution", response.distribution().stream()
                        .map(distribution -> Map.of(
                                "clubCount", distribution.clubCount(),
                                "memberCount", distribution.memberCount()))
                        .toList())
                .log("회원 현황 API 처리를 완료했습니다.");
        return response;
    }
}
