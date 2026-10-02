package yeobaek.backend.content.api;

import yeobaek.backend.foundation.identity.ContentId;

public final class ContentNotFoundFailure extends RuntimeException {

    private final long failedContentIdValue;

    public ContentNotFoundFailure(ContentId contentId) {
        super("컨텐츠가 존재하지 않습니다: contentId=" + contentId.value());
        this.failedContentIdValue = contentId.value();
    }

    public long failedContentId() {
        return failedContentIdValue;
    }
}
