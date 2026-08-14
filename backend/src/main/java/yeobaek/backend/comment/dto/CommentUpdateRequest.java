package yeobaek.backend.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CommentUpdateRequest(
        @Schema(description = "수정할 내용 (1~1000자)") String content
) {
}
