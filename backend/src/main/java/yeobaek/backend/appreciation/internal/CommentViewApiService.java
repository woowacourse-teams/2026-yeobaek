package yeobaek.backend.appreciation.internal;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.appreciation.api.CommentApi;
import yeobaek.backend.appreciation.api.CommentViewApi;
import yeobaek.backend.foundation.identity.AppreciationId;
import yeobaek.backend.foundation.identity.MemberId;

@Service
@RequiredArgsConstructor
public class CommentViewApiService implements CommentViewApi {

    private final CommentApi comments;

    @Override
    @Transactional
    public void markViewed(MemberId actorId, List<AppreciationId> commentIds) {
        if (!commentIds.isEmpty()) {
            comments.markViewed(actorId, commentIds);
        }
    }
}
