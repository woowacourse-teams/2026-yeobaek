package yeobaek.backend.content.api;

import yeobaek.backend.foundation.identity.ContentId;

public final class ContentBodyUnsupportedFailure extends RuntimeException {

    private final long failedContentIdValue;

    public ContentBodyUnsupportedFailure(ContentId contentId, String kind) {
        this(contentId, kind, null);
    }

    public ContentBodyUnsupportedFailure(ContentId contentId, String kind, Throwable cause) {
        super("본문을 제공하지 않는 컨텐츠입니다: contentId=" + contentId.value() + ", kind=" + kind, cause);
        failedContentIdValue = contentId.value();
    }

    public long failedContentId() {
        return failedContentIdValue;
    }
}
