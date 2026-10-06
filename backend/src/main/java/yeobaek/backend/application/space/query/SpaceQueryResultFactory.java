package yeobaek.backend.application.space.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import yeobaek.backend.application.content.ContentCardResult;
import yeobaek.backend.application.space.query.SpaceQueryResult.Progress;
import yeobaek.backend.content.api.ContentApi;
import yeobaek.backend.content.api.location.ContentLocationQueryApi;
import yeobaek.backend.content.api.metadata.ContentMetadataApi;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.reading.api.ReadingProgress;
import yeobaek.backend.space.api.club.ProgressRate;

@Component
@RequiredArgsConstructor
public class SpaceQueryResultFactory {

    private final ContentApi contents;
    private final ContentMetadataApi metadata;
    private final ContentLocationQueryApi locations;

    public ContentCardResult content(ContentId contentId) {
        var content = contents.getContent(contentId);
        var found = metadata.get(contentId);
        return new ContentCardResult(contentId, content.kind(), found.title(),
                found.creators().stream().map(creator -> creator.name()).toList(), found.coverImageUrl(),
                found.unitCount(), content.available());
    }

    public Progress progress(ReadingProgress progress, int unitCount) {
        if (progress == null) {
            return null;
        }
        int sequence = locations.get(progress.locationId()).passageSequence();
        return new Progress(sequence, ProgressRate.calculate(sequence, unitCount).roundedPercentage(),
                progress.lastReadAt());
    }
}
