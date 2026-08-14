package yeobaek.backend.club.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ClubJoinResponse(
        @Schema(description = "모임 ID") Long clubId,
        @Schema(description = "모임 이름") String name,
        @Schema(description = "모임 도서") ClubBookResponse book
) {
}
