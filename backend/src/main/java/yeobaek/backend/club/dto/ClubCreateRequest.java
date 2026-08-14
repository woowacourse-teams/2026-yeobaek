package yeobaek.backend.club.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ClubCreateRequest(
        @Schema(description = "모임 이름 (1~20자, 공백만은 불가)") String name,
        @Schema(description = "읽을 도서 ID (영구 고정)") Long bookId
) {
}
