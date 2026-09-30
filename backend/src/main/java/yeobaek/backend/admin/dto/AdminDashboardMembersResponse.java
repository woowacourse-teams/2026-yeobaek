package yeobaek.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

public record AdminDashboardMembersResponse(
        @Schema(description = "회원별 참여 모임 현황") List<AdminDashboardMemberResponse> members,
        @Schema(description = "전체 회원의 평균 참여 모임 수", example = "1.25") BigDecimal averageClubCount,
        @Schema(description = "참여 모임 수별 회원 수")
        List<AdminDashboardClubCountDistributionResponse> distribution
) {

    public AdminDashboardMembersResponse {
        members = List.copyOf(members);
        distribution = List.copyOf(distribution);
    }
}
