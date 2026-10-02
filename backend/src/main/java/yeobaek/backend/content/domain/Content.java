package yeobaek.backend.content.domain;

import yeobaek.backend.foundation.identity.ContentId;

public interface Content {

    ContentId id();

    String kind();

    boolean available();
}
