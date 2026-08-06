package watson.backend.club.dto;

import java.time.LocalDateTime;

public record MyProgressResponse(int lastReadPassageSequence, int progressRate, LocalDateTime lastReadAt) {
}
