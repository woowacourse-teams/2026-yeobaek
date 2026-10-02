package yeobaek.backend.content.api;

import yeobaek.backend.foundation.identity.ContentId;

public final class ContentUnavailableFailure extends RuntimeException {

    private final long failedContentIdValue;

    public ContentUnavailableFailure(ContentId contentId) {
        super("더 이상 이용할 수 없는 컨텐츠입니다: contentId=" + contentId.value());
        this.failedContentIdValue = contentId.value();
    }

    public long failedContentId() {
        return failedContentIdValue;
    }
}
