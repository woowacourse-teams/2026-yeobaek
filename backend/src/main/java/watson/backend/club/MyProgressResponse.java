package watson.backend.club;

import java.time.LocalDateTime;

public record MyProgressResponse(int lastReadPassageSequence, int progressRate, LocalDateTime lastReadAt) {
}
