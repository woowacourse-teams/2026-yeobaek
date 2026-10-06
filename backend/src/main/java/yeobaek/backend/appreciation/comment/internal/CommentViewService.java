package yeobaek.backend.appreciation.comment.internal;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.appreciation.api.comment.CommentViewApi;
import yeobaek.backend.appreciation.comment.domain.CommentView;
import yeobaek.backend.appreciation.comment.repository.CommentRepository;
import yeobaek.backend.appreciation.comment.repository.CommentViewRepository;
import yeobaek.backend.shared.identity.AppreciationId;
import yeobaek.backend.shared.identity.MemberId;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentViewService implements CommentViewApi {

    private final CommentRepository comments;
    private final CommentViewRepository views;

    @Override
    public void markViewed(MemberId actorId, List<AppreciationId> commentIds) {
        if (commentIds.isEmpty()) {
            return;
        }
        List<Long> ids = commentIds.stream().map(AppreciationId::value).toList();
        var viewed = new HashSet<>(views.findViewedCommentIds(actorId.value(), ids));
        views.saveAll(comments.findAllById(ids).stream()
                .filter(comment -> !viewed.contains(comment.getId()))
                .map(comment -> new CommentView(actorId.value(), comment.getId()))
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Set<AppreciationId> findViewedIds(MemberId actorId, Collection<AppreciationId> commentIds) {
        if (commentIds.isEmpty()) {
            return Set.of();
        }
        return views.findViewedCommentIds(actorId.value(), commentIds.stream().map(AppreciationId::value).toList())
                .stream()
                .map(AppreciationId::new)
                .collect(Collectors.toSet());
    }
}
