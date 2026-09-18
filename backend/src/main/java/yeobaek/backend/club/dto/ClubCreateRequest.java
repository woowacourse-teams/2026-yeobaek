package yeobaek.backend.club.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import yeobaek.backend.club.domain.vo.ClubName;

public record ClubCreateRequest(
        @JsonProperty(required = true)
        @JsonSetter(nulls = Nulls.FAIL)
        @Schema(type = "string", description = "모임 이름 (1~20자, 공백만은 불가)") @NotNull ClubName name,
        @Schema(description = "읽을 도서 ID (영구 고정)") @NotNull Long bookId
) {
}
