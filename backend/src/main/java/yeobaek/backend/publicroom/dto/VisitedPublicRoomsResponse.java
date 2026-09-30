package yeobaek.backend.publicroom.dto;

import java.util.List;

public record VisitedPublicRoomsResponse(List<VisitedPublicRoomResponse> publicRooms) {

    public VisitedPublicRoomsResponse {
        publicRooms = List.copyOf(publicRooms);
    }
}
