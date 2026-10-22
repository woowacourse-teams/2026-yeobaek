package yeobaek.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import yeobaek.backend.book.domain.vo.BookDescription;

public record BookDescriptionUpdateRequest(
        @Schema(type = "string", description = "검수한 책 소개. 누락/null은 소개 제거, 공백 문자열은 거부", nullable = true)
        BookDescription description
) {
}
