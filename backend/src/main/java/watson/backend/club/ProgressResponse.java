package watson.backend.club;

import java.time.LocalDateTime;

public record ProgressResponse(int lastReadPassageSequence, int progressRate, LocalDateTime lastReadAt) {
}
