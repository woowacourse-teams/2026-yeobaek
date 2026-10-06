package yeobaek.backend.web.club.dto;

import yeobaek.backend.web.common.dto.BookResponse;

import io.swagger.v3.oas.annotations.media.Schema;

public record ClubJoinResponse(
        @Schema(description = "모임 ID") Long clubId,
        @Schema(description = "모임 이름") String name,
        @Schema(description = "모임 도서") BookResponse book
) {
}
