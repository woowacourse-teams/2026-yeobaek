package yeobaek.backend.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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
public class AdminBookCoverController {

    private final BookCoverUploadService bookCoverUploadService;

    @Operation(summary = "도서 표지 업로드 URL 발급",
            description = "발급된 URL로 requiredHeaders를 포함한 PUT 요청을 전송한 뒤 coverImageKey를 도서 API에 전달한다.")
    @PostMapping("/api/admin/book-covers/upload-url")
    public BookCoverUploadUrlResponse issueUploadUrl(@RequestBody BookCoverUploadUrlRequest request) {
        return bookCoverUploadService.issueUploadUrl(request);
    }
}
