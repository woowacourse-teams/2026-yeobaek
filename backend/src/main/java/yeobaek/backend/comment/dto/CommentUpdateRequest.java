package yeobaek.backend.comment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import io.swagger.v3.oas.annotations.media.Schema;
import yeobaek.backend.comment.domain.vo.CommentContent;

public record CommentUpdateRequest(
        @JsonProperty(required = true)
        @JsonSetter(nulls = Nulls.FAIL)
        @Schema(type = "string", description = "수정할 내용 (1~1000자)") CommentContent content
) {
}
