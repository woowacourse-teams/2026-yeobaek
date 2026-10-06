package yeobaek.backend.appreciation.internal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import yeobaek.backend.appreciation.api.lifecycle.AppreciationRootApi;
import yeobaek.backend.appreciation.api.AppreciationKind;
import yeobaek.backend.appreciation.internal.erasure.AppreciationSubtypeEraser;
import yeobaek.backend.appreciation.internal.erasure.UnsupportedAppreciationKindException;
import yeobaek.backend.shared.identity.AppreciationId;
import yeobaek.backend.shared.identity.MemberId;

class AppreciationDataEraseServiceTest {

    private static final MemberId AUTHOR = new MemberId(7L);
    private final AppreciationRootApi roots = mock(AppreciationRootApi.class);

    @Test
    void dispatchesEverySubtypeBeforeErasingRoots() {
        AppreciationSubtypeEraser comments = mock(AppreciationSubtypeEraser.class);
        AppreciationSubtypeEraser notes = mock(AppreciationSubtypeEraser.class);
        AppreciationId commentId = new AppreciationId(10L);
        AppreciationId noteId = new AppreciationId(11L);
        AppreciationKind note = new AppreciationKind("NOTE");
        given(roots.findAuthoredBy(AUTHOR)).willReturn(List.of(root(commentId, AppreciationKind.COMMENT),
                root(noteId, note)));
        Map<AppreciationKind, AppreciationSubtypeEraser> erasers = new LinkedHashMap<>();
        erasers.put(AppreciationKind.COMMENT, comments);
        erasers.put(note, notes);

        new AppreciationDataEraseService(roots, erasers).eraseAuthoredBy(AUTHOR);

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
        var service = new AppreciationDataEraseService(roots, Map.of(AppreciationKind.COMMENT, comments));

        assertThatThrownBy(() -> service.eraseAuthoredBy(AUTHOR))
                .isInstanceOf(UnsupportedAppreciationKindException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class);

        verify(comments, never()).eraseBodies(org.mockito.ArgumentMatchers.anyList());
        verify(roots, never()).eraseAuthoredBy(AUTHOR);
    }

    @Test
    void delegatesActorViewCleanupToEverySubtype() {
        AppreciationSubtypeEraser comments = mock(AppreciationSubtypeEraser.class);
        AppreciationSubtypeEraser notes = mock(AppreciationSubtypeEraser.class);
        Map<AppreciationKind, AppreciationSubtypeEraser> erasers = new LinkedHashMap<>();
        erasers.put(AppreciationKind.COMMENT, comments);
        erasers.put(new AppreciationKind("NOTE"), notes);

        new AppreciationDataEraseService(roots, erasers).eraseViewsBy(AUTHOR);

        verify(comments).eraseViewsBy(AUTHOR);
        verify(notes).eraseViewsBy(AUTHOR);
    }

    @Test
    void delegatesActorReportCleanupToEverySubtype() {
        AppreciationSubtypeEraser comments = mock(AppreciationSubtypeEraser.class);
        AppreciationSubtypeEraser notes = mock(AppreciationSubtypeEraser.class);
        Map<AppreciationKind, AppreciationSubtypeEraser> erasers = new LinkedHashMap<>();
        erasers.put(AppreciationKind.COMMENT, comments);
        erasers.put(new AppreciationKind("NOTE"), notes);

        new AppreciationDataEraseService(roots, erasers).eraseReportsBy(AUTHOR);

        verify(comments).eraseReportsBy(AUTHOR);
        verify(notes).eraseReportsBy(AUTHOR);
    }

    private AppreciationRootApi.Root root(AppreciationId id, AppreciationKind kind) {
        return new AppreciationRootApi.Root(id, AUTHOR, kind, LocalDateTime.of(2026, 10, 2, 12, 0), null);
    }
}
