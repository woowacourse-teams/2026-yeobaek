package yeobaek.backend.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import yeobaek.backend.admin.dto.BookCoverUpdateRequest;
import yeobaek.backend.admin.dto.BookUploadRequest;
import yeobaek.backend.admin.dto.BookUploadResponse;
import yeobaek.backend.admin.service.AdminBookService;
import yeobaek.backend.admin.service.BookIngestService;

@Tag(name = "관리자")
@SecurityRequirement(name = "adminToken")
@RestController
@RequiredArgsConstructor
public class AdminBookController {

    private final BookIngestService bookIngestService;
    private final AdminBookService adminBookService;

    @Operation(summary = "도서 업로드 (인제스트 규격 JSON)",
            description = "본문 순서는 배열 등장 순서로 서버가 1..N을 부여한다. 앱은 사용하지 않는다.")
    @PostMapping("/api/admin/books")
    @ResponseStatus(HttpStatus.CREATED)
    public BookUploadResponse upload(@RequestBody BookUploadRequest request) {
        return bookIngestService.upload(request);
    }

    @Operation(summary = "도서 삭제", description = "도서를 소프트 삭제한다. 연결된 모임과 댓글은 보존한다.")
    @DeleteMapping("/api/admin/books/{bookId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long bookId) {
        adminBookService.delete(bookId);
    }

    @Operation(summary = "도서 표지 교체", description = "기존 S3 객체는 삭제하지 않고 새 객체 키로 교체한다.")
    @PutMapping("/api/admin/books/{bookId}/cover")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void replaceCoverImage(@PathVariable Long bookId, @RequestBody BookCoverUpdateRequest request) {
        adminBookService.replaceCoverImage(bookId, request.coverImageKey());
    }

    @Operation(summary = "도서 표지 제거", description = "도서의 표지 키만 제거하며 기존 S3 객체는 삭제하지 않는다.")
    @DeleteMapping("/api/admin/books/{bookId}/cover")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeCoverImage(@PathVariable Long bookId) {
        adminBookService.removeCoverImage(bookId);
    }
}
