package yeobaek.backend.publicroom.dto;

import yeobaek.backend.club.dto.ClubBookResponse;

public record PublicRoomResponse(
        Long publicRoomId,
        ClubBookResponse book,
        PublicRoomProgressResponse myProgress
) {
}
