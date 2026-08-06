package watson.backend.club.dto;

import java.time.LocalDateTime;

public record LastReadingResponse(
        Long clubId,
        String clubName,
        ClubBookResponse book,
        int lastReadPassageSequence,
        int progressRate,
        LocalDateTime lastReadAt
) {
}
