package yeobaek.backend.club.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ClubJoinRequest(
        @Schema(description = "참여 코드") @NotNull String joinCode
) {
}
