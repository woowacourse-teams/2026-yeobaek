package yeobaek.backend.web.publicroom.dto;

import java.time.LocalDateTime;
import yeobaek.backend.web.common.dto.BookResponse;

public record RecentReadingResponse(
        ReadingSpaceResponse space,
        BookResponse book,
        int lastReadPassageSequence,
        int progressRate,
        LocalDateTime lastReadAt
) {
}
