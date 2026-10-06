package yeobaek.backend.content.api.location;

import java.util.Map;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.shared.exception.LogContextException;
import yeobaek.backend.shared.identity.ContentLocationId;

public class ContentLocationNotFoundException extends LogContextException {

    public ContentLocationNotFoundException(ContentLocationId locationId, String message) {
        super(ErrorCode.CONTENT_LOCATION_NOT_FOUND, message,
                Map.of("locationId", Long.toString(locationId.value())));
    }
}
