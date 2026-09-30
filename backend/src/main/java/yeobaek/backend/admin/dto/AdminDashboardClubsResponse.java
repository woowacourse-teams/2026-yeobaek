package yeobaek.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record AdminDashboardClubsResponse(
        @Schema(description = "모임별 현황") List<AdminDashboardClubResponse> clubs
) {

    public AdminDashboardClubsResponse {
        clubs = List.copyOf(clubs);
    }
}
