package yeobaek.backend.book.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ChapterResponse(
        @Schema(description = "목차 ID") Long chapterId,
        @Schema(description = "목차 제목") String title,
        @Schema(description = "목차 순서") int sequence,
        @Schema(description = "챕터 시작 본문의 전체 순서") int startPassageSequence,
        @Schema(description = "챕터 끝 본문의 전체 순서") int endPassageSequence
) {
}
