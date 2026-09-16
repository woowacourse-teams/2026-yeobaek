package yeobaek.backend.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import io.swagger.v3.oas.annotations.media.Schema;
import yeobaek.backend.book.domain.vo.SentenceContent;

public record SentenceUploadRequest(
        @JsonProperty(required = true)
        @JsonSetter(nulls = Nulls.FAIL)
        @Schema(type = "string", description = "원문의 공백과 개행을 보존한 문장 내용 (필수)") SentenceContent content
) {
}
