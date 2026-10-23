package yeobaek.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import yeobaek.backend.book.domain.vo.BookDescription;

public record BookDescriptionUpdateRequest(
        @Schema(type = "string", description = "교체할 책 소개. 누락/null 및 공백 문자열은 거부")
        @NotNull BookDescription description
) {
}
