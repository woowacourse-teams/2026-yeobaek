package yeobaek.backend.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import yeobaek.backend.book.domain.vo.ChapterTitle;

public record ChapterUploadRequest(
        @JsonProperty(required = true)
        @JsonSetter(nulls = Nulls.FAIL)
        @Schema(type = "string", description = "목차 제목 (1~100자)") ChapterTitle title,
        @Schema(description = "본문 목록 (등장 순서로 전체 순서 부여)") List<PassageUploadRequest> passages
) {

    public ChapterUploadRequest {
        passages = passages == null ? List.of() : List.copyOf(passages);
    }
}
