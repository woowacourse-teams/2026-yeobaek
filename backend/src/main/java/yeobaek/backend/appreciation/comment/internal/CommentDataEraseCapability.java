package yeobaek.backend.appreciation.comment.internal;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.appreciation.internal.erasure.AppreciationSubtypeEraser;
import yeobaek.backend.appreciation.api.AppreciationKind;
import yeobaek.backend.appreciation.comment.repository.CommentReportRepository;
import yeobaek.backend.appreciation.comment.repository.CommentRepository;
import yeobaek.backend.appreciation.comment.repository.CommentViewRepository;
import yeobaek.backend.shared.identity.AppreciationId;
import yeobaek.backend.shared.identity.MemberId;

@Service
@RequiredArgsConstructor
@Transactional
class CommentDataEraseCapability implements AppreciationSubtypeEraser {

    private final CommentRepository commentRepository;
    private final CommentViewRepository viewRepository;
    private final CommentReportRepository reportRepository;

    @Override
    public AppreciationKind supportedKind() {
        return AppreciationKind.COMMENT;
    }

    @Override
    public void eraseBodies(List<AppreciationId> appreciationIds) {
        List<Long> ids = appreciationIds.stream().map(AppreciationId::value).toList();
        viewRepository.deleteAllByAppreciationIdIn(ids);
        reportRepository.deleteAllByAppreciationIdIn(ids);
        commentRepository.deleteAllByAppreciationIdIn(ids);
    }

    @Override
    public void eraseViewsBy(MemberId actorId) {
        viewRepository.deleteAllByActorId(actorId.value());
    }

    @Override
    public void eraseReportsBy(MemberId actorId) {
        reportRepository.deleteAllByReporterId(actorId.value());
    }
}
