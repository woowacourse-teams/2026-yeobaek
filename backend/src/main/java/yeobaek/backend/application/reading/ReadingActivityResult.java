package yeobaek.backend.application.reading;

import java.time.LocalDateTime;
import yeobaek.backend.application.content.ContentCardResult;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.space.api.SpaceKind;

public record ReadingActivityResult(ReadingSpace space, ContentCardResult content, int lastReadPassageSequence,
                                    int progressRate, LocalDateTime lastReadAt) {

    public record ReadingSpace(SpaceId spaceId, SpaceKind kind, Data data) {

        public interface Data {
        }

        public record Club(String name) implements Data {
        }

        public record PublicRoom() implements Data {
        }
    }

}
