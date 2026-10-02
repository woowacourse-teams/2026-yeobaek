package yeobaek.backend.appreciation.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import yeobaek.backend.appreciation.api.AppreciationRootApi;
import yeobaek.backend.appreciation.api.CommentFailure;
import yeobaek.backend.comment.domain.Comment;
import yeobaek.backend.comment.domain.vo.CommentContent;
import yeobaek.backend.comment.internal.CommentApiService;
import yeobaek.backend.comment.repository.CommentReportRepository;
import yeobaek.backend.comment.repository.CommentRepository;
import yeobaek.backend.comment.repository.CommentViewRepository;
import yeobaek.backend.foundation.identity.AppreciationId;
import yeobaek.backend.foundation.identity.MemberId;

class CommentApiServiceTest {

    private final CommentRepository comments = mock(CommentRepository.class);
    private final CommentViewRepository views = mock(CommentViewRepository.class);
    private final CommentReportRepository reports = mock(CommentReportRepository.class);
    private final AppreciationRootApi roots = mock(AppreciationRootApi.class);
    private final CommentApiService api = new CommentApiService(comments, views, reports, roots);
    private Comment entity;
    private AppreciationRootApi.Root root;

    @BeforeEach
    void setUp() {
        entity = new Comment(10L, new CommentContent("원문"));
        root = new AppreciationRootApi.Root(new AppreciationId(10L), new MemberId(2L),
                yeobaek.backend.appreciation.domain.Comment.COMMENT_KIND,
                java.time.LocalDateTime.of(2026, 10, 2, 12, 0), null);
    }

    @Test
    void updateRejectsNonOwnerAtModuleBoundary() {
        given(comments.findById(10L)).willReturn(Optional.of(entity));
        given(roots.getForUpdate(new AppreciationId(10L))).willReturn(root);

        assertThatThrownBy(() -> api.update(new MemberId(1L), new AppreciationId(10L), "변경"))
                .isInstanceOfSatisfying(CommentFailure.class,
                        failure -> assertThat(failure.reason()).isEqualTo(CommentFailure.Reason.NOT_OWNER));

        assertThat(entity.getContent()).isEqualTo("원문");
    }

    @Test
    void deleteRejectsNonOwnerAtModuleBoundary() {
        given(comments.findById(10L)).willReturn(Optional.of(entity));
        given(roots.getForUpdate(new AppreciationId(10L))).willReturn(root);

        assertThatThrownBy(() -> api.delete(new MemberId(1L), new AppreciationId(10L)))
                .isInstanceOfSatisfying(CommentFailure.class,
                        failure -> assertThat(failure.reason()).isEqualTo(CommentFailure.Reason.NOT_OWNER));

        verify(comments, never()).delete(entity);
    }

    @Test
    void reportLocksAndRejectsSelfAtModuleBoundary() {
        given(comments.findById(10L)).willReturn(Optional.of(entity));
        given(roots.getForUpdate(new AppreciationId(10L))).willReturn(root);

        assertThatThrownBy(() -> api.report(new MemberId(2L), new AppreciationId(10L)))
                .isInstanceOfSatisfying(CommentFailure.class,
                        failure -> assertThat(failure.reason())
                                .isEqualTo(CommentFailure.Reason.CANNOT_REPORT_OWN_COMMENT));

        verify(reports, never()).insertIfAbsent(2L, 10L);
    }

    @Test
    void reportIsIdempotentWhileHoldingCommentLock() {
        given(comments.findById(10L)).willReturn(Optional.of(entity));
        given(roots.getForUpdate(new AppreciationId(10L))).willReturn(root);
        given(reports.insertIfAbsent(1L, 10L)).willReturn(0);

        assertThat(api.report(new MemberId(1L), new AppreciationId(10L))).isFalse();

        verify(roots).getForUpdate(new AppreciationId(10L));
        verify(reports).insertIfAbsent(1L, 10L);
    }
}
