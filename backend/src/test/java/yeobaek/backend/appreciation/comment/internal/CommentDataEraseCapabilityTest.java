package yeobaek.backend.appreciation.comment.internal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import yeobaek.backend.appreciation.comment.repository.CommentReportRepository;
import yeobaek.backend.appreciation.comment.repository.CommentRepository;
import yeobaek.backend.appreciation.comment.repository.CommentViewRepository;
import yeobaek.backend.shared.identity.MemberId;

class CommentDataEraseCapabilityTest {

    private static final MemberId ACTOR = new MemberId(7L);

    private final CommentRepository comments = mock(CommentRepository.class);
    private final CommentViewRepository views = mock(CommentViewRepository.class);
    private final CommentReportRepository reports = mock(CommentReportRepository.class);
    private final CommentDataEraseCapability eraser = new CommentDataEraseCapability(comments, views, reports);

    @Test
    void eraseViewsDeletesOnlyActorViewRecords() {
        eraser.eraseViewsBy(ACTOR);

        verify(views).deleteAllByActorId(ACTOR.value());
        verifyNoInteractions(reports, comments);
    }

    @Test
    void eraseReportsDeletesOnlyReporterRecords() {
        eraser.eraseReportsBy(ACTOR);

        verify(reports).deleteAllByReporterId(ACTOR.value());
        verifyNoInteractions(views, comments);
    }
}
