package yeobaek.backend.publicroom.dto;

import java.time.LocalDateTime;
import yeobaek.backend.club.dto.ClubBookResponse;

public record RecentReadingResponse(
        ReadingSpaceResponse space,
        ClubBookResponse book,
        int lastReadPassageSequence,
        int progressRate,
        LocalDateTime lastReadAt
) {
}
