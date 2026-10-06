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
import yeobaek.backend.appreciation.api.lifecycle.AppreciationRootApi;
import yeobaek.backend.appreciation.api.AppreciationKind;
import yeobaek.backend.appreciation.spi.erasure.AppreciationSubtypeEraser;
import yeobaek.backend.appreciation.spi.erasure.AppreciationSubtypeEraserRegistry;
import yeobaek.backend.appreciation.spi.erasure.UnsupportedAppreciationKindException;
import yeobaek.backend.shared.identity.AppreciationId;
import yeobaek.backend.shared.identity.MemberId;

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
        AppreciationKind note = new AppreciationKind("NOTE");
        given(roots.findAuthoredBy(AUTHOR)).willReturn(List.of(root(commentId, AppreciationKind.COMMENT),
                root(noteId, note)));
        given(registry.get(AppreciationKind.COMMENT)).willReturn(comments);
        given(registry.get(note)).willReturn(notes);

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
                root(new AppreciationId(10L), AppreciationKind.COMMENT),
                root(new AppreciationId(11L), new AppreciationKind("NOTE"))));
        given(registry.get(AppreciationKind.COMMENT)).willReturn(comments);
        given(registry.get(new AppreciationKind("NOTE")))
                .willThrow(new UnsupportedAppreciationKindException(new AppreciationKind("NOTE"),
                        "저장된 감상 타입을 삭제할 구현을 찾을 수 없습니다: kind=NOTE", null));

        assertThatThrownBy(() -> service.eraseAuthoredBy(AUTHOR))
                .isInstanceOf(UnsupportedAppreciationKindException.class);

        verify(comments, never()).eraseBodies(org.mockito.ArgumentMatchers.anyList());
        verify(roots, never()).eraseAuthoredBy(AUTHOR);
    }

    @Test
    void delegatesActorViewCleanupToEverySubtype() {
        AppreciationSubtypeEraser comments = mock(AppreciationSubtypeEraser.class);
        AppreciationSubtypeEraser notes = mock(AppreciationSubtypeEraser.class);
        given(registry.all()).willReturn(List.of(comments, notes));

        service.eraseViewsBy(AUTHOR);

        verify(comments).eraseViewsBy(AUTHOR);
        verify(notes).eraseViewsBy(AUTHOR);
    }

    @Test
    void delegatesActorReportCleanupToEverySubtype() {
        AppreciationSubtypeEraser comments = mock(AppreciationSubtypeEraser.class);
        AppreciationSubtypeEraser notes = mock(AppreciationSubtypeEraser.class);
        given(registry.all()).willReturn(List.of(comments, notes));

        service.eraseReportsBy(AUTHOR);

        verify(comments).eraseReportsBy(AUTHOR);
        verify(notes).eraseReportsBy(AUTHOR);
    }

    private AppreciationRootApi.Root root(AppreciationId id, AppreciationKind kind) {
        return new AppreciationRootApi.Root(id, AUTHOR, kind, LocalDateTime.of(2026, 10, 2, 12, 0), null);
    }
}
