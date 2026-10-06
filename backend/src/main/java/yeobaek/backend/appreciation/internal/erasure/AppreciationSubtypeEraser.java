package yeobaek.backend.appreciation.internal.erasure;

import java.util.List;
import yeobaek.backend.appreciation.api.AppreciationKind;
import yeobaek.backend.shared.identity.AppreciationId;
import yeobaek.backend.shared.identity.MemberId;

public interface AppreciationSubtypeEraser {

    AppreciationKind supportedKind();

    void eraseBodies(List<AppreciationId> appreciationIds);

    void eraseViewsBy(MemberId actorId);

    void eraseReportsBy(MemberId actorId);
}
