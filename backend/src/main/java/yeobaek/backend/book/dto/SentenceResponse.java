package yeobaek.backend.book.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record SentenceResponse(
        @Schema(description = "문장 ID") Long sentenceId,
        @Schema(description = "문단 안에서의 순서") int sequence,
        @Schema(description = "원문 공백과 개행을 보존한 문장 텍스트") String content,
        @Schema(description = "이 모임에서 문장에 작성된 댓글 수") long commentCount
) {
}
