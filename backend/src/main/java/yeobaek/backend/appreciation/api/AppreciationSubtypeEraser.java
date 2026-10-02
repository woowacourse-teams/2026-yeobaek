package yeobaek.backend.appreciation.api;

import java.util.List;
import yeobaek.backend.foundation.identity.AppreciationId;
import yeobaek.backend.foundation.identity.MemberId;

public interface AppreciationSubtypeEraser {

    String supportedKind();

    void eraseBodies(List<AppreciationId> appreciationIds);

    void eraseReactionsBy(MemberId actorId);
}
