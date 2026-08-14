package yeobaek.backend.club.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record MyClubResponse(
        @Schema(description = "모임 ID") Long clubId,
        @Schema(description = "모임 이름") String name,
        @Schema(description = "모임 회원 수") long memberCount,
        @Schema(description = "모임 도서") ClubBookResponse book,
        @Schema(description = "내 진도 (읽기 시작 전이면 null)", nullable = true) MyProgressResponse myProgress
) {
}
