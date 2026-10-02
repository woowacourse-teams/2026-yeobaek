package yeobaek.backend.web.compatibility;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import yeobaek.backend.application.publicroom.PublicRoomWorkflow;
import yeobaek.backend.application.publicroom.PublicRoomWorkflow.BookResult;
import yeobaek.backend.application.publicroom.PublicRoomWorkflow.ProgressResult;
import yeobaek.backend.application.publicroom.PublicRoomWorkflow.PublicRoomWorkflowFailure;
import yeobaek.backend.application.publicroom.PublicRoomWorkflow.RoomResult;
import yeobaek.backend.book.domain.BookStatus;
import yeobaek.backend.book.dto.PassageResponse;
import yeobaek.backend.book.dto.PassagesResponse;
import yeobaek.backend.book.dto.SentenceResponse;
import yeobaek.backend.book.service.BookCoverUrlResolver;
import yeobaek.backend.club.dto.ClubBookResponse;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.publicroom.dto.PublicRoomDetailResponse;
import yeobaek.backend.publicroom.dto.PublicRoomProgressResponse;
import yeobaek.backend.publicroom.dto.PublicRoomResponse;
import yeobaek.backend.publicroom.dto.PublicRoomSort;
import yeobaek.backend.publicroom.dto.PublicRoomsResponse;
import yeobaek.backend.publicroom.dto.VisitedPublicRoomResponse;
import yeobaek.backend.publicroom.dto.VisitedPublicRoomsResponse;
import yeobaek.backend.support.BadRequestException;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.NotFoundException;

@Service
@RequiredArgsConstructor
public class PublicRoomService {

    private final PublicRoomWorkflow workflow;
    private final BookCoverUrlResolver coverUrlResolver;

    public PublicRoomsResponse findAll(Long memberId, PublicRoomSort sort) {
        try {
            return new PublicRoomsResponse(workflow.findAllMostVisited(new MemberId(memberId)).stream()
                    .map(this::toResponse).toList());
        } catch (PublicRoomWorkflowFailure failure) {
            throw toLegacyFailure(failure);
        }
    }

    public VisitedPublicRoomsResponse findVisited(Long memberId) {
        try {
            return new VisitedPublicRoomsResponse(workflow.findVisited(new MemberId(memberId)).stream()
                    .map(visited -> new VisitedPublicRoomResponse(visited.room().publicRoomId(),
                            toBook(visited.room().book()), toProgress(visited.room().progress()),
                            visited.lastVisitedAt()))
                    .toList());
        } catch (PublicRoomWorkflowFailure failure) {
            throw toLegacyFailure(failure);
        }
    }

    public PublicRoomDetailResponse findDetail(Long memberId, Long publicRoomId) {
        try {
            var detail = workflow.findDetail(new MemberId(memberId), publicRoomId);
            return new PublicRoomDetailResponse(detail.room().publicRoomId(), toBook(detail.room().book()),
                    toProgress(detail.room().progress()), detail.lastVisitedAt());
        } catch (PublicRoomWorkflowFailure failure) {
            throw toLegacyFailure(failure);
        }
    }

    public void visit(Long memberId, Long publicRoomId) {
        try {
            workflow.visit(new MemberId(memberId), publicRoomId, LocalDateTime.now());
        } catch (PublicRoomWorkflowFailure failure) {
            throw toLegacyFailure(failure);
        }
    }

    public PassagesResponse findPassages(Long memberId, Long publicRoomId, int from, int to) {
        try {
            return new PassagesResponse(workflow.findPassages(new MemberId(memberId), publicRoomId, from, to).stream()
                    .map(passage -> new PassageResponse(passage.passageId(), passage.sequence(), passage.chapterId(),
                            passage.sentences().stream().map(sentence -> new SentenceResponse(sentence.sentenceId(),
                                    sentence.sequence(), sentence.content(), sentence.commentCount())).toList()))
                    .toList());
        } catch (PublicRoomWorkflowFailure failure) {
            throw toLegacyFailure(failure);
        }
    }

    public PublicRoomProgressResponse updateProgress(Long memberId, Long publicRoomId, Long passageId) {
        try {
            return toProgress(workflow.updateProgress(new MemberId(memberId), publicRoomId, passageId,
                    LocalDateTime.now()));
        } catch (PublicRoomWorkflowFailure failure) {
            throw toLegacyFailure(failure);
        }
    }

    private PublicRoomResponse toResponse(RoomResult room) {
        return new PublicRoomResponse(room.publicRoomId(), toBook(room.book()), toProgress(room.progress()));
    }

    private ClubBookResponse toBook(BookResult book) {
        return new ClubBookResponse(book.bookId(), book.title(), book.authors(),
                coverUrlResolver.resolve(book.coverImageKey()), book.passageCount(), BookStatus.valueOf(book.status()));
    }

    private PublicRoomProgressResponse toProgress(ProgressResult progress) {
        if (progress == null) {
            return null;
        }
        return new PublicRoomProgressResponse(progress.passageSequence(), progress.progressRate(),
                progress.lastReadAt());
    }

    private RuntimeException toLegacyFailure(PublicRoomWorkflowFailure failure) {
        return switch (failure.reason()) {
            case PUBLIC_ROOM_NOT_FOUND -> new NotFoundException(ErrorCode.PUBLIC_ROOM_NOT_FOUND,
                    failure.getMessage());
            case PASSAGE_NOT_FOUND -> new NotFoundException(ErrorCode.PASSAGE_NOT_FOUND, failure.getMessage());
            case BOOK_NOT_AVAILABLE -> new BadRequestException(ErrorCode.BOOK_NOT_AVAILABLE, failure.getMessage(),
                    failure.context());
            case INVALID_RANGE -> new IllegalArgumentException(failure.getMessage());
        };
    }
}
