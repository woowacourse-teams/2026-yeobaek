package yeobaek.backend.club.dto;

public record MyClubResponse(
        Long clubId,
        String name,
        long memberCount,
        ClubBookResponse book,
        MyProgressResponse myProgress
) {
}
