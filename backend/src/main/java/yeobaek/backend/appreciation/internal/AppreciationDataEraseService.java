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
import yeobaek.backend.appreciation.spi.erasure.AppreciationSubtypeEraser;
import yeobaek.backend.appreciation.spi.erasure.AppreciationSubtypeEraserRegistry;
import yeobaek.backend.shared.identity.AppreciationId;
import yeobaek.backend.shared.identity.MemberId;

@Service
@RequiredArgsConstructor
@Transactional
public class AppreciationDataEraseService implements AppreciationDataEraser {

    private final AppreciationRootApi roots;
    private final AppreciationSubtypeEraserRegistry erasers;

    @Override
    public void eraseAuthoredBy(MemberId authorId) {
        Map<AppreciationKind, List<AppreciationId>> idsByKind = groupByKind(roots.findAuthoredBy(authorId));
        Map<AppreciationKind, AppreciationSubtypeEraser> resolved = new LinkedHashMap<>();
        idsByKind.keySet().forEach(kind -> resolved.put(kind, erasers.get(kind)));
        idsByKind.forEach((kind, ids) -> resolved.get(kind).eraseBodies(ids));
        roots.eraseAuthoredBy(authorId);
    }

    @Override
    public void eraseViewsBy(MemberId actorId) {
        erasers.all().forEach(eraser -> eraser.eraseViewsBy(actorId));
    }

    @Override
    public void eraseReportsBy(MemberId actorId) {
        erasers.all().forEach(eraser -> eraser.eraseReportsBy(actorId));
    }

    private Map<AppreciationKind, List<AppreciationId>> groupByKind(List<AppreciationRootApi.Root> authored) {
        return authored.stream().collect(java.util.stream.Collectors.groupingBy(
                AppreciationRootApi.Root::kind,
                LinkedHashMap::new,
                java.util.stream.Collectors.mapping(AppreciationRootApi.Root::id,
                        java.util.stream.Collectors.toList())));
    }
}
