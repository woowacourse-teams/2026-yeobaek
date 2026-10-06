package yeobaek.backend.appreciation.api.lifecycle;

import java.time.LocalDateTime;
import java.util.List;
import yeobaek.backend.appreciation.api.AppreciationKind;
import yeobaek.backend.shared.identity.AppreciationId;
import yeobaek.backend.shared.identity.MemberId;

public interface AppreciationRootApi {

    Root create(AppreciationKind kind, MemberId authorId, LocalDateTime createdAt);

    Root get(AppreciationId appreciationId);

    Root getForUpdate(AppreciationId appreciationId);

    void markUpdated(AppreciationId appreciationId, LocalDateTime updatedAt);

    List<Root> findAuthoredBy(MemberId authorId);

    void delete(AppreciationId appreciationId);

    void eraseAuthoredBy(MemberId authorId);

    record Root(AppreciationId id, MemberId authorId, AppreciationKind kind,
                LocalDateTime createdAt, LocalDateTime updatedAt) {
    }
}
