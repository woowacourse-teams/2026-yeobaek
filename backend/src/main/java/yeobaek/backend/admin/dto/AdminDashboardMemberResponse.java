package yeobaek.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AdminDashboardMemberResponse(
        @Schema(description = "회원 ID") Long memberId,
        @Schema(description = "닉네임") String nickname,
        @Schema(description = "현재 참여 중인 모임 수") long clubCount
) {
}
