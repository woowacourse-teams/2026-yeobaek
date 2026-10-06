package yeobaek.backend.appreciation.comment.internal;

import java.time.LocalDateTime;
import yeobaek.backend.appreciation.api.comment.CommentResponse;
import yeobaek.backend.appreciation.api.AppreciationKind;
import yeobaek.backend.shared.identity.AppreciationId;
import yeobaek.backend.shared.identity.MemberId;

public record CommentSnapshot(AppreciationId id, MemberId authorId, String content,
                              LocalDateTime createdAt, LocalDateTime updatedAt) implements CommentResponse {

    @Override
    public AppreciationKind kind() {
        return AppreciationKind.COMMENT;
    }
}
