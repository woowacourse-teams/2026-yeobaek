package yeobaek.backend.club.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ClubJoinRequest(
        @Schema(description = "참여 코드") String joinCode
) {
}
