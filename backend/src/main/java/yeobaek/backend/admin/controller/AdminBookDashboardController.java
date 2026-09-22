package yeobaek.backend.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import yeobaek.backend.admin.dto.AdminDashboardBooksResponse;
import yeobaek.backend.admin.service.AdminBookDashboardService;

@Tag(name = "관리자 대시보드")
@SecurityRequirement(name = "adminToken")
@RestController
@RequiredArgsConstructor
public class AdminBookDashboardController {

    private final AdminBookDashboardService adminBookDashboardService;

    @Operation(summary = "도서별 모임 현황 조회", description = "삭제된 도서와 모임이 없는 도서를 포함해 모임 수를 조회한다.")
    @GetMapping("/api/admin/dashboard/books")
    public AdminDashboardBooksResponse findBooks() {
        return adminBookDashboardService.findBooks();
    }
}
