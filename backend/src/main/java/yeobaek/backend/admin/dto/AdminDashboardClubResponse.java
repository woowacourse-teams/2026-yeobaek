package yeobaek.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import yeobaek.backend.book.domain.BookStatus;

public record AdminDashboardClubResponse(
        @Schema(description = "모임 ID") Long clubId,
        @Schema(description = "모임 이름") String name,
        @Schema(description = "도서 ID") Long bookId,
        @Schema(description = "도서 제목") String bookTitle,
        @Schema(description = "도서 상태") BookStatus bookStatus,
        @Schema(description = "현재 참여 중인 회원 수") long memberCount,
        @Schema(description = "현재 저장된 댓글 수") long commentCount
) {
}
