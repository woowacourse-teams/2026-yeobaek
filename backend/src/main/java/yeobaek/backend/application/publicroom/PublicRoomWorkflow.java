package yeobaek.backend.application.publicroom;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.foundation.identity.ContentId;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.foundation.identity.SpaceId;
import yeobaek.backend.reading.api.PublicRoomVisitApi;
import yeobaek.backend.application.reading.ReadingProgressWorkflow;
import yeobaek.backend.readmodel.publicroom.PublicRoomReadModel;
import yeobaek.backend.readmodel.publicroom.PublicRoomReadModel.BookSnapshot;
import yeobaek.backend.readmodel.publicroom.PublicRoomReadModel.PassageLocationSnapshot;
import yeobaek.backend.readmodel.publicroom.PublicRoomReadModel.PassageSnapshot;
import yeobaek.backend.readmodel.publicroom.PublicRoomReadModel.ProgressSnapshot;
import yeobaek.backend.readmodel.publicroom.PublicRoomReadModel.RoomSnapshot;

@Service
@RequiredArgsConstructor
public class PublicRoomWorkflow {

    private static final int MAX_RANGE_SIZE = 100;

    private final PublicRoomReadModel rooms;
    private final PublicRoomVisitApi visits;
    private final ReadingProgressWorkflow progress;

    @Transactional(readOnly = true)
    public List<RoomResult> findAllMostVisited(MemberId actorId) {
        List<RoomSnapshot> found = rooms.findAllActive();
        List<SpaceId> spaceIds = found.stream().map(room -> room.space().id()).toList();
        Map<SpaceId, ProgressSnapshot> progressBySpace = rooms.findProgresses(actorId, spaceIds);
        Map<SpaceId, Long> visitorCounts = rooms.countVisitors(spaceIds);
        return found.stream()
                .sorted(Comparator.comparingLong((RoomSnapshot room) ->
                        visitorCounts.getOrDefault(room.space().id(), 0L)).reversed())
                .map(room -> toRoomResult(room, progressBySpace.get(room.space().id())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VisitedRoomResult> findVisited(MemberId actorId) {
        return rooms.findVisitedActive(actorId).stream()
                .map(visited -> new VisitedRoomResult(toRoomResult(visited.room(), visited.progress()),
                        visited.lastVisitedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public RoomDetailResult findDetail(MemberId actorId, Long publicRoomId) {
        RoomSnapshot room = requireRoom(publicRoomId);
        ProgressSnapshot foundProgress = rooms.findProgress(actorId, room.space().id(), room.book().contentId())
                .orElse(null);
        LocalDateTime lastVisitedAt = rooms.findLastVisit(actorId, room.space().id()).orElse(null);
        return new RoomDetailResult(toRoomResult(room, foundProgress), lastVisitedAt);
    }

    @Transactional
    public void visit(MemberId actorId, Long publicRoomId, LocalDateTime visitedAt) {
        RoomSnapshot room = requireLockedRoom(publicRoomId);
        requireAvailable(room.book());
        visits.visit(actorId, room.space().id(), visitedAt);
    }

    @Transactional(readOnly = true)
    public List<PassageResult> findPassages(MemberId actorId, Long publicRoomId, int from, int to) {
        if (from < 1 || to < from) {
            throw new PublicRoomWorkflowFailure(FailureReason.INVALID_RANGE, "본문 범위가 올바르지 않습니다.");
        }
        if (to - from + 1 > MAX_RANGE_SIZE) {
            throw new PublicRoomWorkflowFailure(FailureReason.INVALID_RANGE,
                    "본문은 한 번에 최대 " + MAX_RANGE_SIZE + "개까지 조회할 수 있습니다.");
        }
        RoomSnapshot room = requireRoom(publicRoomId);
        requireAvailable(room.book());
        List<PassageSnapshot> passages = rooms.findPassages(room.book().bookId(), from, to);
        List<Long> sentenceIds = passages.stream().flatMap(passage -> passage.sentences().stream())
                .map(PublicRoomReadModel.SentenceSnapshot::sentenceId).toList();
        Map<Long, Long> commentCounts = rooms.countVisibleComments(actorId, publicRoomId, sentenceIds);
        return passages.stream().map(passage -> new PassageResult(passage.passageId(), passage.sequence(),
                passage.chapterId(), passage.sentences().stream().map(sentence -> new SentenceResult(
                        sentence.sentenceId(), sentence.sequence(), sentence.content(),
                        commentCounts.getOrDefault(sentence.sentenceId(), 0L))).toList())).toList();
    }

    @Transactional
    public ProgressResult updateProgress(MemberId actorId, Long publicRoomId, Long passageId,
                                         LocalDateTime readAt) {
        RoomSnapshot room = requireLockedRoom(publicRoomId);
        PassageLocationSnapshot passage = rooms.findPassage(passageId)
                .orElseThrow(() -> new PublicRoomWorkflowFailure(FailureReason.PASSAGE_NOT_FOUND,
                        "진도를 갱신할 본문이 존재하지 않습니다: passageId=" + passageId));
        if (!room.book().bookId().equals(passage.bookId())) {
            throw new PublicRoomWorkflowFailure(FailureReason.PASSAGE_NOT_FOUND,
                    "공개방 도서에 속하지 않는 본문입니다: passageId=" + passageId);
        }
        requireAvailable(room.book());
        var changed = progress.update(actorId, room.space().id(), room.book().contentId(), passage.locationId(), readAt);
        return new ProgressResult(passage.sequence(), progressRate(passage.sequence(), room.book().passageCount()),
                changed.lastReadAt());
    }

    private RoomSnapshot requireRoom(Long publicRoomId) {
        return rooms.findRoom(publicRoomId).orElseThrow(() -> roomNotFound(publicRoomId));
    }

    private RoomSnapshot requireLockedRoom(Long publicRoomId) {
        return rooms.findRoomForUpdate(publicRoomId).orElseThrow(() -> roomNotFound(publicRoomId));
    }

    private PublicRoomWorkflowFailure roomNotFound(Long publicRoomId) {
        return new PublicRoomWorkflowFailure(FailureReason.PUBLIC_ROOM_NOT_FOUND,
                "공개방이 존재하지 않습니다: publicRoomId=" + publicRoomId);
    }

    private void requireAvailable(BookSnapshot book) {
        if (!book.available()) {
            throw new PublicRoomWorkflowFailure(FailureReason.BOOK_NOT_AVAILABLE,
                    "더 이상 이용할 수 없는 도서입니다.",
                    Map.of("bookId", book.bookId().toString(), "bookStatus", book.status()));
        }
    }

    private RoomResult toRoomResult(RoomSnapshot room, ProgressSnapshot foundProgress) {
        ProgressResult result = foundProgress == null ? null : new ProgressResult(foundProgress.passageSequence(),
                progressRate(foundProgress.passageSequence(), room.book().passageCount()),
                foundProgress.lastReadAt());
        return new RoomResult(room.space().publicRoomId(), toBookResult(room.book()), result);
    }

    private BookResult toBookResult(BookSnapshot book) {
        return new BookResult(book.bookId(), book.contentId(), book.title(), book.authors(), book.coverImageKey(),
                book.passageCount(), book.status(), book.available());
    }

    private int progressRate(int passageSequence, int passageCount) {
        if (passageSequence > passageCount) {
            throw new IllegalArgumentException("최근 읽은 본문 순서는 전체 본문 개수를 초과할 수 없습니다.");
        }
        return (int) Math.round(passageSequence * 100.0 / passageCount);
    }

    public record RoomResult(Long publicRoomId, BookResult book, ProgressResult progress) {
    }

    public record RoomDetailResult(RoomResult room, LocalDateTime lastVisitedAt) {
    }

    public record VisitedRoomResult(RoomResult room, LocalDateTime lastVisitedAt) {
    }

    public record BookResult(Long bookId, ContentId contentId, String title, List<String> authors,
                             String coverImageKey, int passageCount, String status, boolean available) {

        public BookResult {
            authors = List.copyOf(authors);
        }
    }

    public record ProgressResult(int passageSequence, int progressRate, LocalDateTime lastReadAt) {
    }

    public record PassageResult(Long passageId, int sequence, Long chapterId, List<SentenceResult> sentences) {

        public PassageResult {
            sentences = List.copyOf(sentences);
        }
    }

    public record SentenceResult(Long sentenceId, int sequence, String content, long commentCount) {
    }

    public enum FailureReason {
        PUBLIC_ROOM_NOT_FOUND,
        PASSAGE_NOT_FOUND,
        BOOK_NOT_AVAILABLE,
        INVALID_RANGE
    }

    public static final class PublicRoomWorkflowFailure extends RuntimeException {

        private final FailureReason failureReason;
        private final Map<String, String> logContext;

        public PublicRoomWorkflowFailure(FailureReason reason, String message) {
            this(reason, message, Map.of());
        }

        public PublicRoomWorkflowFailure(FailureReason reason, String message, Map<String, String> context) {
            super(message);
            this.failureReason = reason;
            this.logContext = Map.copyOf(context);
        }

        public FailureReason reason() {
            return failureReason;
        }

        public Map<String, String> context() {
            return logContext;
        }
    }
}
