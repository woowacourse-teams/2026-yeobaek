package yeobaek.backend.web.v2.appreciation;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.jackson.JacksonMixin;
import yeobaek.backend.appreciation.api.AppreciationKind;
import yeobaek.backend.appreciation.api.comment.CommentContent;

public interface AppreciationRequests {

    public record Write(@NotNull @Schema(description = "감상 종류", example = "COMMENT") AppreciationKind kind,
                        @NotNull @Valid @Schema(description = "종류별 감상 입력. COMMENT는 CommentData") Data data) {
    }

    @Schema(oneOf = CommentData.class)
    public interface Data {
    }

    public record CommentData(@NotNull @Schema(description = "1~1000자 댓글 내용") CommentContent content)
            implements Data {
    }

    @JacksonMixin(Write.class)
    public abstract static class WriteMixin {

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "kind")
        public abstract Data data();
    }
}
