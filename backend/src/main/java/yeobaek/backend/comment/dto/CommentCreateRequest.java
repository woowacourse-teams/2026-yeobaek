package yeobaek.backend.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CommentCreateRequest(
        @Schema(description = "내용 (1~1000자)") String content
) {
}
