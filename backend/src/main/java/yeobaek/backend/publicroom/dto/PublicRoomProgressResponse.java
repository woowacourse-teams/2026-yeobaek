package yeobaek.backend.publicroom.dto;

import java.time.LocalDateTime;

public record PublicRoomProgressResponse(
        int lastReadPassageSequence,
        int progressRate,
        LocalDateTime lastReadAt
) {
}
