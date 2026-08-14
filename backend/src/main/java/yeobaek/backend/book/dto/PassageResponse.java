package yeobaek.backend.book.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record PassageResponse(
        @Schema(description = "본문 ID") Long passageId,
        @Schema(description = "책 전체 기준 순서") int sequence,
        @Schema(description = "소속 목차 ID") Long chapterId,
        @Schema(description = "본문 텍스트") String content,
        @Schema(description = "이 모임의 댓글 수") long commentCount
) {
}
