package yeobaek.backend.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record BlockedMemberResponse(
        @Schema(description = "차단된 회원 ID") Long memberId,
        @Schema(description = "차단된 회원 닉네임") String nickname
) {
}
