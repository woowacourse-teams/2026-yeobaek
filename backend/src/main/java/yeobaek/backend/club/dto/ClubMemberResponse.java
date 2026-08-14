package yeobaek.backend.club.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ClubMemberResponse(
        @Schema(description = "회원 ID") Long memberId,
        @Schema(description = "닉네임") String nickname,
        @Schema(description = "요청자 본인 여부") boolean mine
) {
}
