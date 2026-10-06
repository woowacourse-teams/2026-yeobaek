package yeobaek.backend.web.publicroom.dto;

import yeobaek.backend.web.common.dto.BookResponse;

public record PublicRoomResponse(
        Long publicRoomId,
        BookResponse book,
        PublicRoomProgressResponse myProgress
) {
}
