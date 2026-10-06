package yeobaek.backend.web.publicroom.dto;

public record PublicRoomReadingSpaceResponse(
        String type,
        Long publicRoomId
) implements ReadingSpaceResponse {

    public PublicRoomReadingSpaceResponse(Long publicRoomId) {
        this("PUBLIC_ROOM", publicRoomId);
    }
}
