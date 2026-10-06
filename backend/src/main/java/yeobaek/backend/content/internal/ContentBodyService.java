package yeobaek.backend.content.internal;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.content.api.ContentApi;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.content.api.body.ContentBodyApi;
import yeobaek.backend.content.spi.body.ContentBodyProvider;
import yeobaek.backend.content.spi.body.ContentBodyProviderRegistry;
import yeobaek.backend.content.api.body.ContentBodyUnsupportedException;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.ContentLocationId;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentBodyService implements ContentBodyApi {

    private final ContentApi contentApi;
    private final ContentBodyProviderRegistry providers;

    @Override
    public List<Passage> findPassages(ContentId contentId, int from, int to) {
        return provider(contentId).findPassages(contentId, from, to);
    }

    @Override
    public Optional<Passage> findPassage(ContentId contentId, long passageId) {
        return provider(contentId).findPassage(passageId);
    }

    @Override
    public Optional<Passage> findPassageByLocation(ContentId contentId, ContentLocationId locationId) {
        return provider(contentId).findPassageByLocation(locationId);
    }

    @Override
    public int passageCount(ContentId contentId) {
        return provider(contentId).passageCount(contentId);
    }

    private ContentBodyProvider provider(ContentId contentId) {
        ContentKind kind = contentApi.getContent(contentId).kind();
        try {
            return providers.get(kind);
        } catch (IllegalArgumentException exception) {
            throw new ContentBodyUnsupportedException(contentId, kind,
                    "본문을 제공하지 않는 컨텐츠입니다: contentId=" + contentId.value() + ", kind=" + kind,
                    exception);
        }
    }
}
