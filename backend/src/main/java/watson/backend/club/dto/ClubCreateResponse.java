package watson.backend.club.dto;

public record ClubCreateResponse(Long clubId, String name, String joinCode, ClubBookResponse book) {
}
