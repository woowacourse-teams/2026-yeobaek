package yeobaek.backend.content.api;

import yeobaek.backend.shared.identity.ContentId;

public interface Content {

    ContentId id();

    ContentKind kind();

    boolean available();
}
