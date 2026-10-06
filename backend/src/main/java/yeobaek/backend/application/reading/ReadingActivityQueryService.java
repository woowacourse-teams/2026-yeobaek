package yeobaek.backend.application.reading;

import java.util.Comparator;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.application.content.ContentCardResult;
import yeobaek.backend.application.reading.ReadingActivityResult.ReadingSpace;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.readmodel.reading.RecentReadingReadModel;
import yeobaek.backend.readmodel.reading.RecentReadingReadModel.RecentReadingSnapshot;
import yeobaek.backend.space.api.SpaceKind;
import yeobaek.backend.space.api.club.ProgressRate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadingActivityQueryService {

    private final RecentReadingReadModel readings;
    private final ReadingSpaceDataRegistry spaceData;

    public Optional<ReadingActivityResult> findLastClub(MemberId actorId) {
        return readings.findCandidates(actorId).stream()
                .filter(reading -> SpaceKind.CLUB.equals(reading.space().kind()))
                .max(Comparator.comparing(RecentReadingSnapshot::lastReadAt))
                .map(this::result);
    }

    public Optional<ReadingActivityResult> findRecent(MemberId actorId) {
        return readings.findCandidates(actorId).stream()
                .max(Comparator.comparing(RecentReadingSnapshot::lastReadAt))
                .map(this::result);
    }

    private ReadingActivityResult result(RecentReadingSnapshot reading) {
        var content = reading.content();
        var card = new ContentCardResult(content.contentId(), content.kind(), content.title(), content.creators(),
                content.coverImageUrl(), content.unitCount(), content.available());
        var space = new ReadingSpace(reading.space().id(), reading.space().kind(),
                spaceData.get(reading.space().kind()).create(reading.space(), reading.spaceName()));
        return new ReadingActivityResult(space, card, reading.passageSequence(),
                ProgressRate.calculate(reading.passageSequence(), content.unitCount()).roundedPercentage(),
                reading.lastReadAt());
    }
}
