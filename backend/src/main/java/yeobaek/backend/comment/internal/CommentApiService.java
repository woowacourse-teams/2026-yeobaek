package yeobaek.backend.comment.internal;

import java.util.HashSet;
import java.util.List;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.appreciation.api.CommentApi;
import yeobaek.backend.appreciation.api.CommentFailure;
import yeobaek.backend.appreciation.api.AppreciationRootApi;
import yeobaek.backend.appreciation.api.AppreciationNotFoundFailure;
import yeobaek.backend.appreciation.domain.Comment;
import yeobaek.backend.comment.domain.CommentView;
import yeobaek.backend.comment.domain.vo.CommentContent;
import yeobaek.backend.comment.repository.CommentReportRepository;
import yeobaek.backend.comment.repository.CommentRepository;
import yeobaek.backend.comment.repository.CommentViewRepository;
import yeobaek.backend.foundation.identity.AppreciationId;
import yeobaek.backend.foundation.identity.MemberId;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentApiService implements CommentApi {

    private final CommentRepository commentRepository;
    private final CommentViewRepository viewRepository;
    private final CommentReportRepository reportRepository;
    private final AppreciationRootApi appreciationRoots;
    private final Clock clock = Clock.systemDefaultZone();

    @Override
    public Comment create(MemberId authorId, String content) {
        var root = appreciationRoots.create(Comment.COMMENT_KIND, authorId, java.time.LocalDateTime.now(clock));
        var entity = commentRepository.save(new yeobaek.backend.comment.domain.Comment(
                root.id().value(), new CommentContent(content)));
        return toDomain(entity, root);
    }

    @Override
    @Transactional(readOnly = true)
    public Comment get(AppreciationId commentId) {
        var root = getRoot(commentId, false);
        return toDomain(findBody(commentId), root);
    }

    @Override
    public Comment getForUpdate(AppreciationId commentId) {
        var root = getRoot(commentId, true);
        return toDomain(findBody(commentId), root);
    }

    @Override
    public Comment update(MemberId actorId, AppreciationId commentId, String content) {
        var root = getRoot(commentId, true);
        var entity = findBody(commentId);
        requireOwner(root, actorId, commentId);
        Comment changed = toDomain(entity, root);
        changed.update(content, clock);
        entity.updateContent(new CommentContent(changed.content()));
        appreciationRoots.markUpdated(commentId, changed.updatedAt());
        return changed;
    }

    @Override
    public void delete(MemberId actorId, AppreciationId commentId) {
        var root = getRoot(commentId, true);
        var entity = findBody(commentId);
        requireOwner(root, actorId, commentId);
        viewRepository.deleteAllByAppreciationId(commentId.value());
        reportRepository.deleteAllByAppreciationId(commentId.value());
        commentRepository.delete(entity);
        appreciationRoots.delete(commentId);
    }

    @Override
    public void markViewed(MemberId actorId, List<AppreciationId> commentIds) {
        List<Long> ids = commentIds.stream().map(AppreciationId::value).toList();
        var viewed = new HashSet<>(viewRepository.findViewedCommentIds(actorId.value(), ids));
        viewRepository.saveAll(commentRepository.findAllById(ids).stream()
                .filter(comment -> !viewed.contains(comment.getId()))
                .map(comment -> new CommentView(actorId.value(), comment.getId())).toList());
    }

    @Override
    public boolean report(MemberId reporterId, AppreciationId commentId) {
        var root = getRoot(commentId, true);
        findBody(commentId);
        if (root.authorId().equals(reporterId)) {
            throw new CommentFailure(CommentFailure.Reason.CANNOT_REPORT_OWN_COMMENT, commentId);
        }
        return reportRepository.insertIfAbsent(reporterId.value(), commentId.value()) == 1;
    }

    private void requireOwner(AppreciationRootApi.Root root, MemberId actorId,
                              AppreciationId commentId) {
        if (!root.authorId().equals(actorId)) {
            throw new CommentFailure(CommentFailure.Reason.NOT_OWNER, commentId);
        }
    }

    private yeobaek.backend.comment.domain.Comment findBody(AppreciationId commentId) {
        return commentRepository.findById(commentId.value())
                .orElseThrow(() -> new CommentFailure(CommentFailure.Reason.NOT_FOUND, commentId));
    }

    private AppreciationRootApi.Root getRoot(AppreciationId commentId, boolean forUpdate) {
        try {
            return forUpdate ? appreciationRoots.getForUpdate(commentId) : appreciationRoots.get(commentId);
        } catch (AppreciationNotFoundFailure failure) {
            throw new CommentFailure(CommentFailure.Reason.NOT_FOUND, commentId, failure);
        }
    }

    private Comment toDomain(yeobaek.backend.comment.domain.Comment entity, AppreciationRootApi.Root root) {
        return new Comment(new AppreciationId(entity.getId()), root.authorId(), entity.getContent(),
                root.createdAt(), root.updatedAt());
    }
}
