package yeobaek.backend.comment.internal;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.appreciation.api.AppreciationSubtypeEraser;
import yeobaek.backend.appreciation.domain.Comment;
import yeobaek.backend.comment.repository.CommentReportRepository;
import yeobaek.backend.comment.repository.CommentRepository;
import yeobaek.backend.comment.repository.CommentViewRepository;
import yeobaek.backend.foundation.identity.AppreciationId;
import yeobaek.backend.foundation.identity.MemberId;

@Service
@RequiredArgsConstructor
@Transactional
class CommentDataEraseCapability implements AppreciationSubtypeEraser {

    private final CommentRepository commentRepository;
    private final CommentViewRepository viewRepository;
    private final CommentReportRepository reportRepository;

    @Override
    public String supportedKind() {
        return Comment.COMMENT_KIND;
    }

    @Override
    public void eraseBodies(List<AppreciationId> appreciationIds) {
        List<Long> ids = appreciationIds.stream().map(AppreciationId::value).toList();
        viewRepository.deleteAllByAppreciationIdIn(ids);
        reportRepository.deleteAllByAppreciationIdIn(ids);
        commentRepository.deleteAllByAppreciationIdIn(ids);
    }

    @Override
    public void eraseReactionsBy(MemberId actorId) {
        viewRepository.deleteAllByActorId(actorId.value());
        reportRepository.deleteAllByReporterId(actorId.value());
    }
}
