package watson.backend.club.dto;

import java.time.LocalDateTime;

public record ProgressResponse(int lastReadPassageSequence, int progressRate, LocalDateTime lastReadAt) {
}
