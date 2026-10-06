package yeobaek.backend.application.reading;

import org.springframework.stereotype.Component;
import yeobaek.backend.application.reading.ReadingActivityResult.ReadingSpace;
import yeobaek.backend.space.api.Space;
import yeobaek.backend.space.api.SpaceKind;

@Component
public class PublicRoomReadingSpaceDataProvider implements ReadingSpaceDataProvider {

    @Override
    public SpaceKind supportedKind() {
        return SpaceKind.PUBLIC_ROOM;
    }

    @Override
    public ReadingSpace.Data create(Space space, String name) {
        return new ReadingSpace.PublicRoom();
    }
}
