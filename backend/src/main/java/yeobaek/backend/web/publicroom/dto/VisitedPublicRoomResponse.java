package yeobaek.backend.web.publicroom.dto;

import java.time.LocalDateTime;
import yeobaek.backend.web.common.dto.BookResponse;

public record VisitedPublicRoomResponse(
        Long publicRoomId,
        BookResponse book,
        PublicRoomProgressResponse myProgress,
        LocalDateTime lastVisitedAt
) {
}
