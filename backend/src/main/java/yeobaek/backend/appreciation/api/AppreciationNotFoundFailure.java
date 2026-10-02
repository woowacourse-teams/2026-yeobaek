package yeobaek.backend.appreciation.api;

import yeobaek.backend.foundation.identity.AppreciationId;

public final class AppreciationNotFoundFailure extends RuntimeException {

    public AppreciationNotFoundFailure(AppreciationId appreciationId) {
        super("감상이 존재하지 않습니다: appreciationId=" + appreciationId.value());
    }
}
