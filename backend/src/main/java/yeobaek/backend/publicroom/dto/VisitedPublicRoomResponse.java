package yeobaek.backend.publicroom.dto;

import java.time.LocalDateTime;
import yeobaek.backend.club.dto.ClubBookResponse;

public record VisitedPublicRoomResponse(
        Long publicRoomId,
        ClubBookResponse book,
        PublicRoomProgressResponse myProgress,
        LocalDateTime lastVisitedAt
) {
}
