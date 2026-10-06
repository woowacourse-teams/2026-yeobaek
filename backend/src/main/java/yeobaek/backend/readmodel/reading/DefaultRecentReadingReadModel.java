package yeobaek.backend.readmodel.reading;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.content.api.ContentApi;
import yeobaek.backend.content.api.location.ContentLocationQueryApi;
import yeobaek.backend.content.api.metadata.ContentMetadataApi;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.reading.api.ReadingProgress;
import yeobaek.backend.reading.api.ReadingProgressApi;
import yeobaek.backend.readmodel.reading.ReadingCandidateProvider.Candidate;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultRecentReadingReadModel implements RecentReadingReadModel {

    private final ReadingProgressApi readings;
    private final List<ReadingCandidateProvider> candidateProviders;
    private final ContentLocationQueryApi locations;
    private final ContentApi contents;
    private final ContentMetadataApi metadata;

    @Override
    public List<RecentReadingSnapshot> findCandidates(MemberId actorId) {
        Map<SpaceId, Candidate> candidates = candidateProviders.stream()
                .flatMap(provider -> provider.findCandidates(actorId).stream())
                .collect(Collectors.toMap(candidate -> candidate.space().id(), Function.identity()));
        return readings.findByActor(actorId).stream()
                .filter(progress -> candidates.containsKey(progress.spaceId()))
                .map(progress -> snapshot(candidates.get(progress.spaceId()), progress))
                .toList();
    }

    private RecentReadingSnapshot snapshot(Candidate candidate, ReadingProgress progress) {
        var location = locations.get(progress.locationId());
        var content = contents.getContent(progress.contentId());
        var found = metadata.get(progress.contentId());
        var result = new ContentSnapshot(progress.contentId(), content.kind(), found.title(),
                found.creators().stream().map(creator -> creator.name()).toList(), found.coverImageUrl(),
                found.unitCount(), content.available());
        return new RecentReadingSnapshot(candidate.space(), candidate.name(), result, progress.locationId(),
                location.passageSequence(), progress.lastReadAt());
    }
}
