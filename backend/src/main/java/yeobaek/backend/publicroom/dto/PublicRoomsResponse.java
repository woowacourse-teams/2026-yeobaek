package yeobaek.backend.publicroom.dto;

import java.util.List;

public record PublicRoomsResponse(List<PublicRoomResponse> publicRooms) {

    public PublicRoomsResponse {
        publicRooms = List.copyOf(publicRooms);
    }
}
