package yeobaek.backend.appreciation.comment.internal;

import static yeobaek.backend.support.LogField.REASON;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.appreciation.api.comment.CommentApi;
import yeobaek.backend.appreciation.api.comment.CommentResponse;
import yeobaek.backend.appreciation.api.comment.CommentException;
import yeobaek.backend.appreciation.api.lifecycle.AppreciationRootApi;
import yeobaek.backend.appreciation.api.AppreciationKind;
import yeobaek.backend.appreciation.api.AppreciationNotFoundException;
import yeobaek.backend.appreciation.api.comment.CommentContent;
import yeobaek.backend.appreciation.comment.repository.CommentReportRepository;
import yeobaek.backend.appreciation.comment.repository.CommentRepository;
import yeobaek.backend.appreciation.comment.repository.CommentViewRepository;
import yeobaek.backend.shared.identity.AppreciationId;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.exception.ErrorCode;

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
    public CommentSnapshot create(MemberId authorId, CommentContent content) {
        var root = appreciationRoots.create(AppreciationKind.COMMENT, authorId, java.time.LocalDateTime.now(clock));
        var entity = commentRepository.save(new yeobaek.backend.appreciation.comment.persistence.Comment(
                root.id().value(), content));
        return toSnapshot(entity, root);
    }

    @Override
    @Transactional(readOnly = true)
    public CommentSnapshot get(AppreciationId commentId) {
        var root = getRoot(commentId, false);
        return toSnapshot(findBody(commentId), root);
    }

    @Override
    public CommentSnapshot getForUpdate(AppreciationId commentId) {
        var root = getRoot(commentId, true);
        return toSnapshot(findBody(commentId), root);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> findByIds(Collection<AppreciationId> commentIds) {
        if (commentIds.isEmpty()) {
            return List.of();
        }
        return commentRepository.findAllById(commentIds.stream().map(AppreciationId::value).toList()).stream()
                .<CommentResponse>map(body -> {
                    var root = appreciationRoots.get(new AppreciationId(body.getId()));
                    return new CommentSnapshot(root.id(), root.authorId(), body.getContent(), root.createdAt(),
                            root.updatedAt());
                })
                .sorted(Comparator.comparing(CommentResponse::createdAt)
                        .thenComparing(comment -> comment.id().value()))
                .toList();
    }

    @Override
    public CommentSnapshot update(MemberId actorId, AppreciationId commentId, CommentContent content) {
        var root = getRoot(commentId, true);
        var entity = findBody(commentId);
        requireOwner(root, actorId, commentId);
        entity.updateContent(content);
        var updatedAt = java.time.LocalDateTime.now(clock);
        appreciationRoots.markUpdated(commentId, updatedAt);
        return new CommentSnapshot(commentId, root.authorId(), entity.getContent(), root.createdAt(), updatedAt);
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
    public boolean report(MemberId reporterId, AppreciationId commentId) {
        var root = getRoot(commentId, true);
        findBody(commentId);
        if (root.authorId().equals(reporterId)) {
            throw new CommentException(ErrorCode.CANNOT_REPORT_OWN_COMMENT, commentId,
                    "자신이 작성한 댓글은 신고할 수 없습니다: commentId=" + commentId.value(),
                    java.util.Map.of("actorId", Long.toString(reporterId.value()),
                            REASON, "CANNOT_REPORT_OWN_COMMENT"));
        }
        return reportRepository.insertIfAbsent(reporterId.value(), commentId.value()) == 1;
    }

    private void requireOwner(AppreciationRootApi.Root root, MemberId actorId,
                              AppreciationId commentId) {
        if (!root.authorId().equals(actorId)) {
            throw new CommentException(ErrorCode.NOT_COMMENT_OWNER, commentId,
                    "댓글 작성자만 댓글을 변경할 수 있습니다: commentId=" + commentId.value(),
                    java.util.Map.of("actorId", Long.toString(actorId.value()), REASON, "NOT_OWNER"));
        }
    }

    private yeobaek.backend.appreciation.comment.persistence.Comment findBody(AppreciationId commentId) {
        return commentRepository.findById(commentId.value())
                .orElseThrow(() -> new CommentException(ErrorCode.COMMENT_NOT_FOUND, commentId,
                        "댓글 본문이 존재하지 않습니다: commentId=" + commentId.value(),
                        java.util.Map.of(REASON, "NOT_FOUND")));
    }

    private AppreciationRootApi.Root getRoot(AppreciationId commentId, boolean forUpdate) {
        try {
            return forUpdate ? appreciationRoots.getForUpdate(commentId) : appreciationRoots.get(commentId);
        } catch (AppreciationNotFoundException failure) {
            throw new CommentException(ErrorCode.COMMENT_NOT_FOUND, commentId,
                    "댓글이 존재하지 않습니다: commentId=" + commentId.value(),
                    java.util.Map.of(REASON, "NOT_FOUND"), failure);
        }
    }

    private CommentSnapshot toSnapshot(yeobaek.backend.appreciation.comment.persistence.Comment entity, AppreciationRootApi.Root root) {
        return new CommentSnapshot(new AppreciationId(entity.getId()), root.authorId(), entity.getContent(),
                root.createdAt(), root.updatedAt());
    }
}
