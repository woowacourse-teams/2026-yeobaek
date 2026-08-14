package yeobaek.backend.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import yeobaek.backend.admin.dto.BookUploadRequest;
import yeobaek.backend.admin.dto.BookUploadResponse;
import yeobaek.backend.admin.service.BookIngestService;

@Tag(name = "관리자")
@SecurityRequirement(name = "adminToken")
@RestController
@RequiredArgsConstructor
public class AdminBookController {

    private final BookIngestService bookIngestService;

    @Operation(summary = "도서 업로드 (인제스트 규격 JSON)",
            description = "본문 순서는 배열 등장 순서로 서버가 1..N을 부여한다. 앱은 사용하지 않는다.")
    @PostMapping("/api/admin/books")
    @ResponseStatus(HttpStatus.CREATED)
    public BookUploadResponse upload(@RequestBody BookUploadRequest request) {
        return bookIngestService.upload(request);
    }
}
