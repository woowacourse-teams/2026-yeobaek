package yeobaek.backend.appreciation.internal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.appreciation.api.erasure.AppreciationDataEraser;
import yeobaek.backend.appreciation.api.AppreciationKind;
import yeobaek.backend.appreciation.api.lifecycle.AppreciationRootApi;
import yeobaek.backend.appreciation.internal.erasure.AppreciationSubtypeEraser;
import yeobaek.backend.appreciation.internal.erasure.UnsupportedAppreciationKindException;
import yeobaek.backend.shared.identity.AppreciationId;
import yeobaek.backend.shared.identity.MemberId;

@Service
@RequiredArgsConstructor
@Transactional
public class AppreciationDataEraseService implements AppreciationDataEraser {

    private final AppreciationRootApi roots;
    private final Map<AppreciationKind, AppreciationSubtypeEraser> erasers;

    @Override
    public void eraseAuthoredBy(MemberId authorId) {
        Map<AppreciationKind, List<AppreciationId>> idsByKind = groupByKind(roots.findAuthoredBy(authorId));
        Map<AppreciationKind, AppreciationSubtypeEraser> resolved = new LinkedHashMap<>();
        idsByKind.keySet().forEach(kind -> resolved.put(kind, eraser(kind)));
        idsByKind.forEach((kind, ids) -> resolved.get(kind).eraseBodies(ids));
        roots.eraseAuthoredBy(authorId);
    }

    @Override
    public void eraseViewsBy(MemberId actorId) {
        erasers.values().forEach(eraser -> eraser.eraseViewsBy(actorId));
    }

    @Override
    public void eraseReportsBy(MemberId actorId) {
        erasers.values().forEach(eraser -> eraser.eraseReportsBy(actorId));
    }

    private AppreciationSubtypeEraser eraser(AppreciationKind kind) {
        AppreciationSubtypeEraser eraser = erasers.get(kind);
        if (eraser == null) {
            IllegalArgumentException cause = new IllegalArgumentException(
                    "지원하지 않는 capability입니다: " + kind);
            throw new UnsupportedAppreciationKindException(kind,
                    "저장된 감상 타입을 삭제할 구현을 찾을 수 없습니다: kind=" + kind.value(), cause);
        }
        return eraser;
    }

    private Map<AppreciationKind, List<AppreciationId>> groupByKind(List<AppreciationRootApi.Root> authored) {
        return authored.stream().collect(java.util.stream.Collectors.groupingBy(
                AppreciationRootApi.Root::kind,
                LinkedHashMap::new,
                java.util.stream.Collectors.mapping(AppreciationRootApi.Root::id,
                        java.util.stream.Collectors.toList())));
    }
}
