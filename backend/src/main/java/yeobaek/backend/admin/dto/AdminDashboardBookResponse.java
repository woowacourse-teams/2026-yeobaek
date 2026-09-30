package yeobaek.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import yeobaek.backend.book.domain.BookStatus;

public record AdminDashboardBookResponse(
        @Schema(description = "도서 ID") Long bookId,
        @Schema(description = "도서 제목") String title,
        @Schema(description = "도서 상태") BookStatus status,
        @Schema(description = "도서를 읽는 모임 수") long clubCount
) {
}
