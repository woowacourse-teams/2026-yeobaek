package watson.backend.club;

public record MyClubResponse(
        Long clubId,
        String name,
        long memberCount,
        ClubBookResponse book,
        MyProgressResponse myProgress
) {
}
