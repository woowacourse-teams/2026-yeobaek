package yeobaek.backend.appreciation.api;

import java.time.LocalDateTime;
import yeobaek.backend.shared.identity.AppreciationId;
import yeobaek.backend.shared.identity.MemberId;

public interface Appreciation {

    AppreciationId id();

    MemberId authorId();

    LocalDateTime createdAt();

    LocalDateTime updatedAt();

    AppreciationKind kind();
}
