package yeobaek.backend.content.internal.body;

import java.util.List;
import java.util.Optional;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.content.api.body.ContentBodyApi;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.ContentLocationId;

public interface ContentBodyProvider {

    ContentKind supportedKind();

    List<ContentBodyApi.Passage> findPassages(ContentId contentId, int from, int to);

    Optional<ContentBodyApi.Passage> findPassage(long passageId);

    Optional<ContentBodyApi.Passage> findPassageByLocation(ContentLocationId locationId);

    int passageCount(ContentId contentId);
}
