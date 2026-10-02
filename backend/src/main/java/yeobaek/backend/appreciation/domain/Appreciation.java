package yeobaek.backend.appreciation.domain;

import java.time.LocalDateTime;
import yeobaek.backend.foundation.identity.AppreciationId;
import yeobaek.backend.foundation.identity.MemberId;

public interface Appreciation {

    AppreciationId id();

    MemberId authorId();

    LocalDateTime createdAt();

    LocalDateTime updatedAt();

    String kind();
}
