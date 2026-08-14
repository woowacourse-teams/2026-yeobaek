package yeobaek.backend.club.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ClubCreateResponse(
        @Schema(description = "모임 ID") Long clubId,
        @Schema(description = "모임 이름") String name,
        @Schema(description = "참여 코드 (6자 대문자·숫자, 전역 unique, 영구 고정)") String joinCode,
        @Schema(description = "모임 도서") ClubBookResponse book
) {
}
