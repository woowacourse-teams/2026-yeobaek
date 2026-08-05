package watson.backend.club;

public record ClubCreateResponse(Long clubId, String name, String joinCode, ClubBookResponse book) {
}
