package yeobaek.backend.publicroom.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.domain.vo.PassageRange;
import yeobaek.backend.book.dto.PassageResponse;
import yeobaek.backend.book.dto.PassagesResponse;
import yeobaek.backend.book.dto.SentenceResponse;
import yeobaek.backend.book.repository.AuthorBookRepository;
import yeobaek.backend.book.repository.PassageRepository;
import yeobaek.backend.book.service.BookCoverUrlResolver;
import yeobaek.backend.club.dto.ClubBookResponse;
import yeobaek.backend.comment.repository.CommentRepository;
import yeobaek.backend.comment.repository.SentenceCommentCount;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.publicroom.domain.PublicRoom;
import yeobaek.backend.publicroom.domain.PublicRoomActivity;
import yeobaek.backend.publicroom.dto.PublicRoomDetailResponse;
import yeobaek.backend.publicroom.dto.PublicRoomProgressResponse;
import yeobaek.backend.publicroom.dto.PublicRoomResponse;
import yeobaek.backend.publicroom.dto.PublicRoomSort;
import yeobaek.backend.publicroom.dto.PublicRoomsResponse;
import yeobaek.backend.publicroom.dto.VisitedPublicRoomResponse;
import yeobaek.backend.publicroom.dto.VisitedPublicRoomsResponse;
import yeobaek.backend.publicroom.repository.PublicRoomActivityRepository;
import yeobaek.backend.publicroom.repository.PublicRoomRepository;
import yeobaek.backend.publicroom.repository.PublicRoomVisitorCount;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.NotFoundException;

@Service
@RequiredArgsConstructor
public class PublicRoomService {

    private static final int MAX_RANGE_SIZE = 100;

    private final PublicRoomRepository publicRoomRepository;
    private final PublicRoomActivityRepository activityRepository;
    private final MemberRepository memberRepository;
    private final PassageRepository passageRepository;
    private final CommentRepository commentRepository;
    private final AuthorBookRepository authorBookRepository;
    private final BookCoverUrlResolver bookCoverUrlResolver;

    @Transactional(readOnly = true)
    public PublicRoomsResponse findAll(Long memberId, PublicRoomSort sort) {
        List<PublicRoom> rooms = publicRoomRepository.findAllActiveWithBook();
        Map<Long, PublicRoomActivity> activities = activities(memberId, rooms);
        Map<Long, Long> visitorCounts = visitorCounts(rooms);
        Map<Long, List<String>> authorNames = authorNames(rooms.stream()
                .map(room -> room.getBook().getId()).toList());
        Comparator<PublicRoom> comparator = switch (sort) {
            case MOST_VISITED -> Comparator
                    .comparingLong((PublicRoom room) -> visitorCounts.getOrDefault(room.getId(), 0L))
                    .reversed();
        };
        return new PublicRoomsResponse(rooms.stream()
                .sorted(comparator)
                .map(room -> toResponse(room, activities.get(room.getId()), authorNames))
                .toList());
    }

    @Transactional(readOnly = true)
    public VisitedPublicRoomsResponse findVisited(Long memberId) {
        List<PublicRoomActivity> activities = activityRepository.findVisitedActiveByMemberId(memberId);
        Map<Long, List<String>> authorNames = authorNames(activities.stream()
                .map(activity -> activity.getPublicRoom().getBook().getId()).toList());
        return new VisitedPublicRoomsResponse(activities.stream()
                .map(activity -> {
                    PublicRoom room = activity.getPublicRoom();
                    return new VisitedPublicRoomResponse(room.getId(), toBook(room.getBook(), authorNames),
                            toProgress(activity), activity.getLastVisitedAt());
                })
                .toList());
    }

    @Transactional(readOnly = true)
    public PublicRoomDetailResponse findDetail(Long memberId, Long publicRoomId) {
        PublicRoom room = findRoom(publicRoomId);
        PublicRoomActivity activity = activityRepository.findByMemberIdAndPublicRoomId(memberId, publicRoomId)
                .orElse(null);
        Map<Long, List<String>> authorNames = authorNames(List.of(room.getBook().getId()));
        return new PublicRoomDetailResponse(room.getId(), toBook(room.getBook(), authorNames),
                toProgress(activity), activity == null ? null : activity.getLastVisitedAt());
    }

    @Transactional
    public void visit(Long memberId, Long publicRoomId) {
        PublicRoom room = findRoomForUpdate(publicRoomId);
        room.ensureBookAvailable();
        PublicRoomActivity activity = activityRepository.findByMemberIdAndPublicRoomId(memberId, publicRoomId)
                .orElseGet(() -> activityRepository.save(new PublicRoomActivity(
                        memberRepository.getReferenceById(memberId), room)));
        activity.visit(LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public PassagesResponse findPassages(Long memberId, Long publicRoomId, int from, int to) {
        PassageRange range = new PassageRange(from, to);
        if (range.size() > MAX_RANGE_SIZE) {
            throw new IllegalArgumentException("본문은 한 번에 최대 " + MAX_RANGE_SIZE + "개까지 조회할 수 있습니다.");
        }
        PublicRoom room = findRoom(publicRoomId);
        room.ensureBookAvailable();
        List<Passage> passages = passageRepository.findRangeByBookId(
                room.getBook().getId(), range.from(), range.to());
        List<Long> sentenceIds = passages.stream()
                .flatMap(passage -> passage.getSentences().stream())
                .map(sentence -> sentence.getId())
                .toList();
        Map<Long, Long> counts = countComments(memberId, room.getId(), sentenceIds);
        return new PassagesResponse(passages.stream()
                .map(passage -> new PassageResponse(passage.getId(), passage.getSequence().value(),
                        passage.getChapter().getId(), passage.getSentences().stream()
                        .map(sentence -> new SentenceResponse(sentence.getId(), sentence.getSequence().value(),
                                sentence.getContent(), counts.getOrDefault(sentence.getId(), 0L)))
                        .toList()))
                .toList());
    }

    @Transactional
    public PublicRoomProgressResponse updateProgress(Long memberId, Long publicRoomId, Long passageId) {
        PublicRoom room = findRoomForUpdate(publicRoomId);
        Passage passage = passageRepository.findById(passageId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.PASSAGE_NOT_FOUND,
                        "진도를 갱신할 본문이 존재하지 않습니다: passageId=" + passageId));
        if (!room.isReading(passage)) {
            throw new NotFoundException(
                    ErrorCode.PASSAGE_NOT_FOUND,
                    "공개방 도서에 속하지 않는 본문입니다: passageId=" + passageId);
        }
        room.ensureBookAvailable();
        PublicRoomActivity activity = activityRepository.findByMemberIdAndPublicRoomId(memberId, publicRoomId)
                .orElseGet(() -> activityRepository.save(new PublicRoomActivity(
                        memberRepository.getReferenceById(memberId), room)));
        activity.updateProgress(passage, LocalDateTime.now());
        return toProgress(activity);
    }

    private PublicRoom findRoom(Long publicRoomId) {
        return publicRoomRepository.findWithBookById(publicRoomId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.PUBLIC_ROOM_NOT_FOUND,
                        "공개방이 존재하지 않습니다: publicRoomId=" + publicRoomId));
    }

    private PublicRoom findRoomForUpdate(Long publicRoomId) {
        return publicRoomRepository.findWithBookByIdForUpdate(publicRoomId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.PUBLIC_ROOM_NOT_FOUND,
                        "공개방이 존재하지 않습니다: publicRoomId=" + publicRoomId));
    }

    private Map<Long, PublicRoomActivity> activities(Long memberId, List<PublicRoom> rooms) {
        if (rooms.isEmpty()) {
            return Map.of();
        }
        return activityRepository.findAllByMemberIdAndPublicRoomIdIn(
                        memberId, rooms.stream().map(PublicRoom::getId).toList()).stream()
                .collect(Collectors.toMap(activity -> activity.getPublicRoom().getId(), Function.identity()));
    }

    private Map<Long, Long> visitorCounts(List<PublicRoom> rooms) {
        if (rooms.isEmpty()) {
            return Map.of();
        }
        return activityRepository.countVisitorsByPublicRoomIds(rooms.stream().map(PublicRoom::getId).toList()).stream()
                .collect(Collectors.toMap(PublicRoomVisitorCount::getPublicRoomId,
                        PublicRoomVisitorCount::getVisitorCount));
    }

    private Map<Long, List<String>> authorNames(List<Long> bookIds) {
        if (bookIds.isEmpty()) {
            return Map.of();
        }
        return authorBookRepository.findAllWithAuthorByBookIdIn(bookIds).stream()
                .collect(Collectors.groupingBy(authorBook -> authorBook.getBook().getId(),
                        Collectors.mapping(authorBook -> authorBook.getAuthor().getName().value(),
                                Collectors.toList())));
    }

    private PublicRoomResponse toResponse(PublicRoom room, PublicRoomActivity activity,
                                          Map<Long, List<String>> authorNames) {
        return new PublicRoomResponse(room.getId(), toBook(room.getBook(), authorNames), toProgress(activity));
    }

    private ClubBookResponse toBook(Book book, Map<Long, List<String>> authorNames) {
        return ClubBookResponse.of(book, authorNames.getOrDefault(book.getId(), List.of()),
                bookCoverUrlResolver.resolve(book.getCoverImageKey()));
    }

    private PublicRoomProgressResponse toProgress(PublicRoomActivity activity) {
        if (activity == null || !activity.hasProgress()) {
            return null;
        }
        return new PublicRoomProgressResponse(activity.getLastReadPassage().getSequence().value(),
                activity.progressRate(), activity.getLastReadAt());
    }

    private Map<Long, Long> countComments(Long memberId, Long publicRoomId, List<Long> sentenceIds) {
        if (sentenceIds.isEmpty()) {
            return Map.of();
        }
        return commentRepository.countVisibleInPublicRoom(memberId, publicRoomId, sentenceIds).stream()
                .collect(Collectors.toMap(SentenceCommentCount::getSentenceId, SentenceCommentCount::getCommentCount));
    }
}
