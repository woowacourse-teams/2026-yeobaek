package yeobaek.backend.appreciation.api;

import java.time.LocalDateTime;
import java.util.List;
import yeobaek.backend.foundation.identity.AppreciationId;
import yeobaek.backend.foundation.identity.MemberId;

public interface AppreciationRootApi {

    Root create(String kind, MemberId authorId, LocalDateTime createdAt);

    Root get(AppreciationId appreciationId);

    Root getForUpdate(AppreciationId appreciationId);

    void markUpdated(AppreciationId appreciationId, LocalDateTime updatedAt);

    List<Root> findAuthoredBy(MemberId authorId);

    void delete(AppreciationId appreciationId);

    void eraseAuthoredBy(MemberId authorId);

    record Root(AppreciationId id, MemberId authorId, String kind,
                LocalDateTime createdAt, LocalDateTime updatedAt) {
    }
}
