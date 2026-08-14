package yeobaek.backend.club.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record ClubDetailResponse(
        @Schema(description = "모임 ID") Long clubId,
        @Schema(description = "모임 이름") String name,
        @Schema(description = "참여 코드 (전역 unique, 영구 고정)") String joinCode,
        @Schema(description = "모임 도서") ClubBookResponse book,
        @Schema(description = "내 진도 (읽기 시작 전이면 null)", nullable = true) MyProgressResponse myProgress,
        @Schema(description = "참여자 목록 (참여 시각 오름차순)") List<ClubMemberResponse> members
) {

    public ClubDetailResponse {
        members = List.copyOf(members);
    }
}
