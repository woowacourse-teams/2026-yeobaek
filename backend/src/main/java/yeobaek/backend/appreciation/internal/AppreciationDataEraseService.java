package yeobaek.backend.appreciation.internal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.appreciation.api.AppreciationDataEraser;
import yeobaek.backend.appreciation.api.AppreciationRootApi;
import yeobaek.backend.appreciation.api.AppreciationSubtypeEraser;
import yeobaek.backend.appreciation.api.AppreciationSubtypeEraserRegistry;
import yeobaek.backend.foundation.identity.AppreciationId;
import yeobaek.backend.foundation.identity.MemberId;

@Service
@RequiredArgsConstructor
@Transactional
public class AppreciationDataEraseService implements AppreciationDataEraser {

    private final AppreciationRootApi roots;
    private final AppreciationSubtypeEraserRegistry erasers;

    @Override
    public void eraseAuthoredBy(MemberId authorId) {
        Map<String, List<AppreciationId>> idsByKind = groupByKind(roots.findAuthoredBy(authorId));
        Map<String, AppreciationSubtypeEraser> resolved = new LinkedHashMap<>();
        idsByKind.keySet().forEach(kind -> resolved.put(kind, erasers.get(kind)));
        idsByKind.forEach((kind, ids) -> resolved.get(kind).eraseBodies(ids));
        roots.eraseAuthoredBy(authorId);
    }

    @Override
    public void eraseReactionsBy(MemberId actorId) {
        erasers.all().forEach(eraser -> eraser.eraseReactionsBy(actorId));
    }

    private Map<String, List<AppreciationId>> groupByKind(List<AppreciationRootApi.Root> authored) {
        return authored.stream().collect(java.util.stream.Collectors.groupingBy(
                AppreciationRootApi.Root::kind,
                LinkedHashMap::new,
                java.util.stream.Collectors.mapping(AppreciationRootApi.Root::id,
                        java.util.stream.Collectors.toList())));
    }
}
