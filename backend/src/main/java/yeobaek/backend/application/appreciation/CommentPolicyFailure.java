package yeobaek.backend.application.appreciation;

import yeobaek.backend.foundation.identity.ContentId;
import yeobaek.backend.foundation.identity.ContentLocationId;
import yeobaek.backend.foundation.identity.SpaceId;

public final class CommentPolicyFailure extends RuntimeException {

    public enum Reason {
        SPACE_ACCESS_DENIED,
        CONTENT_NOT_BOUND,
        LOCATION_NOT_IN_CONTENT
    }

    private final transient Reason failureReason;

    public CommentPolicyFailure(Reason reason, String message) {
        super(message);
        this.failureReason = reason;
    }

    public Reason reason() {
        return failureReason;
    }

    public static CommentPolicyFailure spaceAccessDenied(SpaceId spaceId) {
        return new CommentPolicyFailure(Reason.SPACE_ACCESS_DENIED,
                "감상을 공유할 공간에 접근할 수 없습니다: spaceId=" + spaceId.value());
    }

    public static CommentPolicyFailure contentNotBound(SpaceId spaceId, ContentId contentId) {
        return new CommentPolicyFailure(Reason.CONTENT_NOT_BOUND,
                "공간에 연결되지 않은 컨텐츠입니다: spaceId=" + spaceId.value()
                        + ", contentId=" + contentId.value());
    }

    public static CommentPolicyFailure locationNotInContent(ContentId contentId, ContentLocationId locationId) {
        return new CommentPolicyFailure(Reason.LOCATION_NOT_IN_CONTENT,
                "컨텐츠에 속하지 않는 감상 위치입니다: contentId=" + contentId.value()
                        + ", locationId=" + locationId.value());
    }
}
