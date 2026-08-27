package yeobaek.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record BookUploadResponse(
        @Schema(description = "생성된 도서 ID") Long bookId,
        @Schema(description = "제목") String title,
        @Schema(description = "표지 이미지 공개 URL", nullable = true) String coverImageUrl,
        @Schema(description = "자동 산출된 본문 개수") int passageCount
) {
}
