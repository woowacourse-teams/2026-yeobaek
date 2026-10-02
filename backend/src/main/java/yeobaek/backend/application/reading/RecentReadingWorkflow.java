package yeobaek.backend.application.reading;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.foundation.identity.ContentId;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.readmodel.reading.RecentReadingReadModel;
import yeobaek.backend.readmodel.reading.RecentReadingReadModel.BookSnapshot;
import yeobaek.backend.readmodel.reading.RecentReadingReadModel.RecentReadingSnapshot;
import yeobaek.backend.space.domain.Space;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecentReadingWorkflow {

    private final RecentReadingReadModel readings;

    public Optional<RecentReadingResult> findRecent(MemberId actorId) {
        return readings.findCandidates(actorId).stream()
                .max(Comparator.comparing(RecentReadingSnapshot::lastReadAt))
                .map(this::toResult);
    }

    private RecentReadingResult toResult(RecentReadingSnapshot reading) {
        BookSnapshot book = reading.book();
        return new RecentReadingResult(reading.space(), reading.spaceName(),
                new BookResult(book.bookId(), book.contentId(), book.title(), book.authors(), book.coverImageKey(),
                        book.passageCount(), book.status(), book.available()),
                reading.passageSequence(), progressRate(reading.passageSequence(), book.passageCount()),
                reading.lastReadAt());
    }

    private int progressRate(int passageSequence, int passageCount) {
        if (passageSequence > passageCount) {
            throw new IllegalArgumentException("최근 읽은 본문 순서는 전체 본문 개수를 초과할 수 없습니다.");
        }
        return (int) Math.round(passageSequence * 100.0 / passageCount);
    }

    public record RecentReadingResult(Space space, String spaceName, BookResult book, int passageSequence,
                                      int progressRate, LocalDateTime lastReadAt) {
    }

    public record BookResult(Long bookId, ContentId contentId, String title, List<String> authors,
                             String coverImageKey, int passageCount, String status, boolean available) {

        public BookResult {
            authors = List.copyOf(authors);
        }
    }
}
