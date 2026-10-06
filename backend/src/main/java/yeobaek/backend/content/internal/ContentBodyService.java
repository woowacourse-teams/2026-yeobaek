package yeobaek.backend.content.internal;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.content.api.ContentApi;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.content.api.body.ContentBodyApi;
import yeobaek.backend.content.api.body.ContentBodyUnsupportedException;
import yeobaek.backend.content.internal.body.ContentBodyProvider;
import yeobaek.backend.shared.identity.ContentId;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentBodyService implements ContentBodyApi {

    private final ContentApi contentApi;
    private final Map<ContentKind, ContentBodyProvider> providers;

    @Override
    public List<Passage> findPassages(ContentId contentId, int from, int to) {
        return provider(contentId).findPassages(contentId, from, to);
    }

    @Override
    public Optional<Passage> findPassage(ContentId contentId, long passageId) {
        return provider(contentId).findPassage(passageId);
    }

    @Override
    public int passageCount(ContentId contentId) {
        return provider(contentId).passageCount(contentId);
    }

    private ContentBodyProvider provider(ContentId contentId) {
        ContentKind kind = contentApi.getContent(contentId).kind();
        try {
            ContentBodyProvider provider = providers.get(kind);
            if (provider == null) {
                throw new IllegalArgumentException("지원하지 않는 capability입니다: " + kind);
            }
            return provider;
        } catch (IllegalArgumentException exception) {
            throw new ContentBodyUnsupportedException(contentId, kind,
                    "본문을 제공하지 않는 컨텐츠입니다: contentId=" + contentId.value() + ", kind=" + kind,
                    exception);
        }
    }
}
