package yeobaek.backend.application.reading;

import yeobaek.backend.application.reading.ReadingActivityResult.ReadingSpace.Data;
import yeobaek.backend.space.api.Space;
import yeobaek.backend.space.api.SpaceKind;

public interface ReadingSpaceDataProvider {

    SpaceKind supportedKind();

    Data create(Space space, String name);
}
