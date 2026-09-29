package yeobaek.backend.admin.controller;

import static yeobaek.backend.support.LogField.ATTEMPT;
import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.PHASE;
import static yeobaek.backend.support.LogField.SUCCESS;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import yeobaek.backend.admin.dto.BookCoverUploadUrlRequest;
import yeobaek.backend.admin.dto.BookCoverUploadUrlResponse;
import yeobaek.backend.admin.service.BookCoverUploadService;

@Tag(name = "관리자")
@SecurityRequirement(name = "adminToken")
@RestController
@RequiredArgsConstructor
@Slf4j
public class AdminBookCoverController {

    private final BookCoverUploadService bookCoverUploadService;

    @Operation(summary = "도서 표지 업로드 URL 발급",
            description = "발급된 URL로 requiredHeaders를 포함한 PUT 요청을 전송한 뒤 coverImageKey를 도서 API에 전달한다.")
    @PostMapping("/api/admin/book-covers/upload-url")
    public BookCoverUploadUrlResponse issueUploadUrl(@Valid @RequestBody BookCoverUploadUrlRequest request) {
        log.atInfo().addKeyValue(OPERATION, "admin.bookCover.issueUploadUrl").addKeyValue(PHASE, ATTEMPT)
                .log("표지 업로드 URL 발급 API 처리를 시작합니다.");
        BookCoverUploadUrlResponse response = bookCoverUploadService.issueUploadUrl(request);
        log.atInfo().addKeyValue(OPERATION, "admin.bookCover.issueUploadUrl").addKeyValue(PHASE, SUCCESS)
                .log("표지 업로드 URL 발급 API 처리를 완료했습니다.");
        return response;
    }
}
