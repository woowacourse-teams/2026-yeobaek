package yeobaek.backend.publicroom.dto;

public record ClubReadingSpaceResponse(
        String type,
        Long clubId,
        String clubName
) implements ReadingSpaceResponse {

    public ClubReadingSpaceResponse(Long clubId, String clubName) {
        this("CLUB", clubId, clubName);
    }
}
