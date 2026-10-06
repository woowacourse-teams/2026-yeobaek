package yeobaek.backend.content.api.location;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.ContentLocationId;

public interface ContentLegacyLocationQueryApi {

    Optional<LocationReference> findSentence(long sentenceId);

    Optional<LocationReference> findPassage(long passageId);

    Optional<LocationReference> findByLocation(ContentLocationId locationId);

    List<SentenceInfo> findSentenceInfo(Collection<ContentLocationId> locationIds);

    record LocationReference(
            ContentLocationId locationId,
            ContentId contentId,
            long passageId,
            int passageSequence
    ) {
    }

    record SentenceInfo(
            ContentLocationId locationId,
            long sentenceId,
            String content,
            long passageId,
            int passageSequence,
            int sentenceSequence
    ) {
    }
}
