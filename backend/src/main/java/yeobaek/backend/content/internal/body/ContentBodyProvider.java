package yeobaek.backend.content.internal.body;

import java.util.List;
import java.util.Optional;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.content.api.body.ContentBodyApi;
import yeobaek.backend.shared.identity.ContentId;

public interface ContentBodyProvider {

    ContentKind supportedKind();

    List<ContentBodyApi.Passage> findPassages(ContentId contentId, int from, int to);

    Optional<ContentBodyApi.Passage> findPassage(long passageId);

    int passageCount(ContentId contentId);
}
