package yeobaek.backend.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record MemberCreateRequest(
        @Schema(description = "닉네임 (1~20자, 공백만은 불가, 중복 불가)") String nickname
) {
}
