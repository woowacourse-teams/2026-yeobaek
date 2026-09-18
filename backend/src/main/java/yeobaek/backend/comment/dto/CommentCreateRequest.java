package yeobaek.backend.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record CommentCreateRequest(
        @Schema(description = "내용 (1~1000자)") @NotNull String content
) {
}
