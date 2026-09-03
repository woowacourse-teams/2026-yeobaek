package yeobaek.backend.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record BlockedMembersResponse(
        @Schema(description = "차단한 회원 목록 (닉네임, 회원 ID 오름차순)") List<BlockedMemberResponse> blockedMembers
) {

    public BlockedMembersResponse {
        blockedMembers = List.copyOf(blockedMembers);
    }
}
