package yeobaek.backend.appreciation.api.comment;

import java.util.Collection;
import java.util.List;
import yeobaek.backend.shared.identity.AppreciationId;
import yeobaek.backend.shared.identity.MemberId;

public interface CommentApi {

    CommentResponse create(MemberId authorId, CommentContent content);

    CommentResponse get(AppreciationId commentId);

    CommentResponse getForUpdate(AppreciationId commentId);

    List<CommentResponse> findByIds(Collection<AppreciationId> commentIds);

    CommentResponse update(MemberId actorId, AppreciationId commentId, CommentContent content);

    void delete(MemberId actorId, AppreciationId commentId);

    boolean report(MemberId reporterId, AppreciationId commentId);
}
