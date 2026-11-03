package yeobaek.backend.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import yeobaek.backend.book.domain.vo.BookHashtags;

public record BookHashtagsUpdateRequest(
        @JsonProperty(required = true)
        @Schema(description = "최종 해시태그 목록. null/빈 배열은 제거한다.", requiredMode = Schema.RequiredMode.REQUIRED)
        BookHashtags hashtags
) {
}
