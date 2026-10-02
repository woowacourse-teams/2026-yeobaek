package yeobaek.backend.content.internal;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.content.api.ContentApi;
import yeobaek.backend.content.api.ContentBodyApi;
import yeobaek.backend.content.api.ContentBodyProvider;
import yeobaek.backend.content.api.ContentBodyProviderRegistry;
import yeobaek.backend.content.api.ContentBodyUnsupportedFailure;
import yeobaek.backend.foundation.identity.ContentId;
import yeobaek.backend.foundation.identity.ContentLocationId;

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
        String kind = contentApi.getContent(contentId).kind();
        try {
            return providers.get(kind);
        } catch (IllegalArgumentException exception) {
            throw new ContentBodyUnsupportedFailure(contentId, kind, exception);
        }
    }
}
