package yeobaek.backend.admin.controller;

import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.RESULT;
import static yeobaek.backend.support.LogField.SUCCESS;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import yeobaek.backend.admin.dto.AdminAuthorsResponse;
import yeobaek.backend.admin.service.AdminAuthorService;

@Tag(name = "관리자")
@SecurityRequirement(name = "adminToken")
@RestController
@RequiredArgsConstructor
@Slf4j
public class AdminAuthorController {

    private final AdminAuthorService adminAuthorService;

    @Operation(summary = "작가 목록 조회", description = "업로드 전 기존 작가 확인용. 등록순, 페이징 없음.")
    @GetMapping("/api/admin/authors")
    public AdminAuthorsResponse findAuthors() {
        log.atInfo().addKeyValue(OPERATION, "admin.author.findAuthors")
                .log("관리자 작가 목록 API 처리를 시작합니다.");
        AdminAuthorsResponse response = adminAuthorService.findAuthors();
        log.atInfo().addKeyValue(OPERATION, "admin.author.findAuthors").addKeyValue(RESULT, SUCCESS)
                .addKeyValue("resultCount", response.authors().size()).log("관리자 작가 목록 API 처리를 완료했습니다.");
        return response;
    }
}
