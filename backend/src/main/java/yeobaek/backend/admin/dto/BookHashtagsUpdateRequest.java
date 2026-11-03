package yeobaek.backend.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import yeobaek.backend.book.domain.vo.BookHashtags;

public record BookHashtagsUpdateRequest(
        @JsonProperty(required = true)
        @Schema(type = "array", description = "최종 해시태그 목록. 최대 5개, 각각 최대 10자. null/빈 배열은 제거한다.",
                nullable = true) BookHashtags hashtags
) {
}
