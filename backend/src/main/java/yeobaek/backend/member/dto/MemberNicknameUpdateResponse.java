package yeobaek.backend.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record MemberNicknameUpdateResponse(
        @Schema(description = "회원 ID") Long memberId,
        @Schema(description = "변경된 닉네임") String nickname
) {
}
