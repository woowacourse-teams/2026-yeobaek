package yeobaek.backend.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

public record BookCategoryUpdateRequest(
        @JsonProperty(required = true)
        @Schema(description = "장르. null 또는 빈 문자열로 저장하면 제거한다.", nullable = true) String category
) {
}
