package yeobaek.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AdminDashboardClubCountDistributionResponse(
        @Schema(description = "참여 중인 모임 수") long clubCount,
        @Schema(description = "해당 모임 수에 속하는 회원 수") long memberCount
) {
}
