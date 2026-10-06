package yeobaek.backend.appreciation.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import yeobaek.backend.appreciation.api.lifecycle.AppreciationRootApi;
import yeobaek.backend.appreciation.api.AppreciationKind;
import yeobaek.backend.appreciation.api.comment.CommentException;
import yeobaek.backend.appreciation.comment.persistence.Comment;
import yeobaek.backend.appreciation.api.comment.CommentContent;
import yeobaek.backend.appreciation.comment.internal.CommentApiService;
import yeobaek.backend.appreciation.comment.repository.CommentReportRepository;
import yeobaek.backend.appreciation.comment.repository.CommentRepository;
import yeobaek.backend.appreciation.comment.repository.CommentViewRepository;
import yeobaek.backend.shared.identity.AppreciationId;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.exception.ErrorCode;

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
                AppreciationKind.COMMENT,
                java.time.LocalDateTime.of(2026, 10, 2, 12, 0), null);
    }

    @Test
    void updateRejectsNonOwnerAtModuleBoundary() {
        given(comments.findById(10L)).willReturn(Optional.of(entity));
        given(roots.getForUpdate(new AppreciationId(10L))).willReturn(root);

        assertThatThrownBy(() -> api.update(new MemberId(1L), new AppreciationId(10L),
                new CommentContent("변경")))
                .isInstanceOfSatisfying(CommentException.class,
                        failure -> assertThat(failure.getCode()).isEqualTo(ErrorCode.NOT_COMMENT_OWNER));

        assertThat(entity.getContent()).isEqualTo("원문");
    }

    @Test
    void deleteRejectsNonOwnerAtModuleBoundary() {
        given(comments.findById(10L)).willReturn(Optional.of(entity));
        given(roots.getForUpdate(new AppreciationId(10L))).willReturn(root);

        assertThatThrownBy(() -> api.delete(new MemberId(1L), new AppreciationId(10L)))
                .isInstanceOfSatisfying(CommentException.class,
                        failure -> assertThat(failure.getCode()).isEqualTo(ErrorCode.NOT_COMMENT_OWNER));

        verify(comments, never()).delete(entity);
    }

    @Test
    void reportLocksAndRejectsSelfAtModuleBoundary() {
        given(comments.findById(10L)).willReturn(Optional.of(entity));
        given(roots.getForUpdate(new AppreciationId(10L))).willReturn(root);

        assertThatThrownBy(() -> api.report(new MemberId(2L), new AppreciationId(10L)))
                .isInstanceOfSatisfying(CommentException.class,
                        failure -> assertThat(failure.getCode())
                                .isEqualTo(ErrorCode.CANNOT_REPORT_OWN_COMMENT));

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

    @Test
    void findByIdsKeepsBulkLookupAndSortsByCreatedAtThenId() {
        var later = new Comment(11L, new CommentContent("나중 댓글"));
        var sameTimeLargerId = new Comment(12L, new CommentContent("같은 시각 댓글"));
        var createdAt = java.time.LocalDateTime.of(2026, 10, 2, 12, 0);
        given(comments.findAllById(List.of(11L, 12L, 10L))).willReturn(List.of(later, sameTimeLargerId, entity));
        given(roots.get(new AppreciationId(11L))).willReturn(new AppreciationRootApi.Root(
                new AppreciationId(11L), new MemberId(2L), AppreciationKind.COMMENT,
                createdAt.plusMinutes(1), null));
        given(roots.get(new AppreciationId(12L))).willReturn(new AppreciationRootApi.Root(
                new AppreciationId(12L), new MemberId(2L), AppreciationKind.COMMENT, createdAt, null));
        given(roots.get(new AppreciationId(10L))).willReturn(new AppreciationRootApi.Root(
                new AppreciationId(10L), new MemberId(2L), AppreciationKind.COMMENT, createdAt, null));

        var found = api.findByIds(List.of(new AppreciationId(11L), new AppreciationId(12L),
                new AppreciationId(10L)));

        assertThat(found).extracting(response -> response.id().value()).containsExactly(10L, 12L, 11L);
    }

    @Test
    void findByIdsDoesNotAccessRepositoriesForEmptyInput() {
        assertThat(api.findByIds(List.of())).isEmpty();

        verify(comments, never()).findAllById(List.of());
    }
}
