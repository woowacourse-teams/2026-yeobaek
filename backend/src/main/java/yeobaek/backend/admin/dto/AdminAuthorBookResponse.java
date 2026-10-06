package yeobaek.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AdminAuthorBookResponse(
        @Schema(description = "작품 도서 ID") Long bookId,
        @Schema(description = "작품 제목") String title,
        @Schema(description = "표지 이미지 공개 URL", nullable = true) String coverImageUrl,
        @Schema(description = "도서 상태", allowableValues = {"ACTIVE", "DELETED"}) String status
) {
}
