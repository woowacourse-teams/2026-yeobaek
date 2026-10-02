package yeobaek.backend.appreciation.internal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import yeobaek.backend.appreciation.api.AppreciationRootApi;
import yeobaek.backend.appreciation.api.AppreciationSubtypeEraser;
import yeobaek.backend.appreciation.api.AppreciationSubtypeEraserRegistry;
import yeobaek.backend.appreciation.api.UnsupportedAppreciationKindFailure;
import yeobaek.backend.foundation.identity.AppreciationId;
import yeobaek.backend.foundation.identity.MemberId;

class AppreciationDataEraseServiceTest {

    private static final MemberId AUTHOR = new MemberId(7L);
    private final AppreciationRootApi roots = mock(AppreciationRootApi.class);
    private final AppreciationSubtypeEraserRegistry registry = mock(AppreciationSubtypeEraserRegistry.class);
    private final AppreciationDataEraseService service = new AppreciationDataEraseService(roots, registry);

    @Test
    void dispatchesEverySubtypeBeforeErasingRoots() {
        AppreciationSubtypeEraser comments = mock(AppreciationSubtypeEraser.class);
        AppreciationSubtypeEraser notes = mock(AppreciationSubtypeEraser.class);
        AppreciationId commentId = new AppreciationId(10L);
        AppreciationId noteId = new AppreciationId(11L);
        given(roots.findAuthoredBy(AUTHOR)).willReturn(List.of(root(commentId, "COMMENT"), root(noteId, "NOTE")));
        given(registry.get("COMMENT")).willReturn(comments);
        given(registry.get("NOTE")).willReturn(notes);

        service.eraseAuthoredBy(AUTHOR);

        var ordered = inOrder(comments, notes, roots);
        ordered.verify(comments).eraseBodies(List.of(commentId));
        ordered.verify(notes).eraseBodies(List.of(noteId));
        ordered.verify(roots).eraseAuthoredBy(AUTHOR);
    }

    @Test
    void rejectsUnregisteredSubtypeBeforeErasingAnyData() {
        AppreciationSubtypeEraser comments = mock(AppreciationSubtypeEraser.class);
        given(roots.findAuthoredBy(AUTHOR)).willReturn(List.of(
                root(new AppreciationId(10L), "COMMENT"), root(new AppreciationId(11L), "NOTE")));
        given(registry.get("COMMENT")).willReturn(comments);
        given(registry.get("NOTE")).willThrow(new UnsupportedAppreciationKindFailure("NOTE"));

        assertThatThrownBy(() -> service.eraseAuthoredBy(AUTHOR))
                .isInstanceOf(UnsupportedAppreciationKindFailure.class);

        verify(comments, never()).eraseBodies(org.mockito.ArgumentMatchers.anyList());
        verify(roots, never()).eraseAuthoredBy(AUTHOR);
    }

    @Test
    void delegatesActorReactionCleanupToEverySubtype() {
        AppreciationSubtypeEraser comments = mock(AppreciationSubtypeEraser.class);
        AppreciationSubtypeEraser notes = mock(AppreciationSubtypeEraser.class);
        given(registry.all()).willReturn(List.of(comments, notes));

        service.eraseReactionsBy(AUTHOR);

        verify(comments).eraseReactionsBy(AUTHOR);
        verify(notes).eraseReactionsBy(AUTHOR);
    }

    private AppreciationRootApi.Root root(AppreciationId id, String kind) {
        return new AppreciationRootApi.Root(id, AUTHOR, kind, LocalDateTime.of(2026, 10, 2, 12, 0), null);
    }
}
