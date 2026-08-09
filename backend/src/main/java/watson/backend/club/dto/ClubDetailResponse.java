package watson.backend.club.dto;

import java.util.List;

public record ClubDetailResponse(
        Long clubId,
        String name,
        String joinCode,
        ClubBookResponse book,
        MyProgressResponse myProgress,
        List<ClubMemberResponse> members
) {

    public ClubDetailResponse {
        members = List.copyOf(members);
    }
}
