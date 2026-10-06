package yeobaek.backend.web.v2.content;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import yeobaek.backend.application.content.ContentCardResult;
import yeobaek.backend.application.content.ContentMetadataQueryService.Detail;
import yeobaek.backend.application.content.ContentMetadataQueryService.Summary;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.web.v2.common.ContentCardResponse;

@Component
public final class ContentWebAdapterRegistry {

    private final Map<ContentKind, ContentWebAdapter> adapters;

    public ContentWebAdapterRegistry(List<ContentWebAdapter> adapters) {
        Map<ContentKind, ContentWebAdapter> registered = new HashMap<>();
        for (ContentWebAdapter adapter : adapters) {
            if (registered.putIfAbsent(adapter.kind(), adapter) != null) {
                throw new IllegalStateException("같은 컨텐츠 종류의 v2 웹 어댑터가 중복 등록되었습니다: " + adapter.kind());
            }
        }
        this.adapters = Map.copyOf(registered);
    }

    public ContentResponses.SummaryResponse summary(Summary result) {
        return get(result.kind()).summary(result);
    }

    public ContentResponses.DetailResponse detail(Detail result) {
        return get(result.kind()).detail(result);
    }

    public ContentCardResponse card(ContentCardResult result) {
        return get(result.kind()).card(result);
    }

    private ContentWebAdapter get(ContentKind kind) {
        ContentWebAdapter adapter = adapters.get(kind);
        if (adapter == null) {
            throw new IllegalArgumentException("지원하지 않는 컨텐츠 종류입니다: " + kind.value());
        }
        return adapter;
    }
}
