package yeobaek.backend.appreciation.api.erasure;

import yeobaek.backend.shared.identity.MemberId;

public interface AppreciationDataEraser {

    void eraseAuthoredBy(MemberId authorId);

    void eraseViewsBy(MemberId actorId);

    void eraseReportsBy(MemberId actorId);
}
