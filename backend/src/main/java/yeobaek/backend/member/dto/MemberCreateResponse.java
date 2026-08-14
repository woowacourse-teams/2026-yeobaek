package yeobaek.backend.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record MemberCreateResponse(
        @Schema(description = "발급된 회원 ID") Long memberId,
        @Schema(description = "닉네임") String nickname
) {
}
