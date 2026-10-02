package yeobaek.backend.appreciation.api;

import yeobaek.backend.foundation.identity.MemberId;

public interface AppreciationDataEraser {

    void eraseAuthoredBy(MemberId authorId);

    void eraseReactionsBy(MemberId actorId);
}
