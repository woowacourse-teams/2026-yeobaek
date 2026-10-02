package yeobaek.backend.content.api;

import java.util.List;
import java.util.Optional;
import yeobaek.backend.foundation.identity.ContentId;
import yeobaek.backend.foundation.identity.ContentLocationId;

public interface ContentBodyProvider {

    String supportedKind();

    List<ContentBodyApi.Passage> findPassages(ContentId contentId, int from, int to);

    Optional<ContentBodyApi.Passage> findPassage(long passageId);

    Optional<ContentBodyApi.Passage> findPassageByLocation(ContentLocationId locationId);

    int passageCount(ContentId contentId);
}
