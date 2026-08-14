package yeobaek.backend.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import yeobaek.backend.admin.dto.AdminAuthorsResponse;
import yeobaek.backend.admin.service.AdminAuthorService;

@Tag(name = "관리자")
@SecurityRequirement(name = "adminToken")
@RestController
@RequiredArgsConstructor
public class AdminAuthorController {

    private final AdminAuthorService adminAuthorService;

    @Operation(summary = "작가 목록 조회", description = "업로드 전 기존 작가 확인용. 등록순, 페이징 없음.")
    @GetMapping("/api/admin/authors")
    public AdminAuthorsResponse findAuthors() {
        return adminAuthorService.findAuthors();
    }
}
