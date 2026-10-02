package yeobaek.backend.web.compatibility;

import static yeobaek.backend.support.LogField.CLUB_ID;
import static yeobaek.backend.support.LogField.COMMENT_ID;
import static yeobaek.backend.support.LogField.CURRENT_PASSAGE_ID;
import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.RESULT;
import static yeobaek.backend.support.LogField.SENTENCE_ID;
import static yeobaek.backend.support.LogField.SUCCESS;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.appreciation.api.CommentFailure;
import yeobaek.backend.application.appreciation.CommentModificationWorkflow;
import yeobaek.backend.application.appreciation.CommentPolicyFailure;
import yeobaek.backend.application.appreciation.CommentQueryWorkflow;
import yeobaek.backend.application.appreciation.CommentQueryWorkflow.CommentQueryFailure;
import yeobaek.backend.application.appreciation.CommentQueryWorkflow.CommentResult;
import yeobaek.backend.application.appreciation.CommentQueryWorkflow.DiscoveryResult;
import yeobaek.backend.application.appreciation.CommentReportWorkflow;
import yeobaek.backend.application.appreciation.CommentSharingWorkflow;
import yeobaek.backend.comment.domain.ContentVisibility;
import yeobaek.backend.comment.domain.vo.CommentContent;
import yeobaek.backend.comment.dto.CommentResponse;
import yeobaek.backend.comment.dto.CommentedSentenceResponse;
import yeobaek.backend.comment.dto.CommentedSentencesResponse;
import yeobaek.backend.comment.dto.CommentsResponse;
import yeobaek.backend.comment.dto.NewCommentCountResponse;
import yeobaek.backend.content.api.ContentNotFoundFailure;
import yeobaek.backend.content.api.ContentUnavailableFailure;
import yeobaek.backend.foundation.identity.AppreciationId;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.support.BadRequestException;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.ForbiddenException;
import yeobaek.backend.support.NotFoundException;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "yeobaek.backend.comment.service.CommentService")
public class CommentService {

    private final CommentSharingWorkflow sharingWorkflow;
    private final CommentReportWorkflow reportWorkflow;
    private final CommentModificationWorkflow modificationWorkflow;
    private final CommentQueryWorkflow queryWorkflow;

    @Transactional
    public CommentsResponse findComments(Long memberId, Long clubId, Long sentenceId) {
        log.atInfo().addKeyValue(OPERATION, "comment.findComments")
                .addKeyValue(CLUB_ID, clubId).addKeyValue(SENTENCE_ID, sentenceId)
                .log("댓글 목록을 조회합니다.");
        var response = commentsResponse(memberId,
                query(() -> queryWorkflow.findClubComments(new MemberId(memberId), clubId, sentenceId)));
        log.atInfo().addKeyValue(OPERATION, "comment.findComments").addKeyValue(RESULT, SUCCESS)
                .addKeyValue(CLUB_ID, clubId).addKeyValue(SENTENCE_ID, sentenceId)
                .addKeyValue("resultCount", response.comments().size()).log("댓글 목록을 조회했습니다.");
        return response;
    }

    @Transactional
    public CommentResponse create(Long memberId, Long clubId, Long sentenceId, CommentContent content) {
        log.atInfo().addKeyValue(OPERATION, "comment.create")
                .addKeyValue(CLUB_ID, clubId).addKeyValue(SENTENCE_ID, sentenceId)
                .log("댓글을 작성합니다.");
        var target = query(() -> queryWorkflow.resolveClubCommentTarget(
                new MemberId(memberId), clubId, sentenceId));
        var shared = command(() -> sharingWorkflow.share(new MemberId(memberId),
                target.spaceId(), target.contentId(), target.locationId(), content.value()));
        var response = CommentResponse.of(shared.comment(), shared.author());
        log.atInfo().addKeyValue(OPERATION, "comment.create").addKeyValue(RESULT, SUCCESS)
                .addKeyValue(CLUB_ID, clubId).addKeyValue(SENTENCE_ID, sentenceId)
                .addKeyValue(COMMENT_ID, response.commentId()).log("댓글을 작성했습니다.");
        return response;
    }

    @Transactional
    public CommentsResponse findPublicRoomComments(Long memberId, Long publicRoomId, Long sentenceId) {
        return commentsResponse(memberId,
                query(() -> queryWorkflow.findPublicRoomComments(new MemberId(memberId), publicRoomId, sentenceId)));
    }

    @Transactional
    public CommentResponse createInPublicRoom(Long memberId, Long publicRoomId, Long sentenceId,
                                               CommentContent content) {
        var target = query(() -> queryWorkflow.resolvePublicRoomCommentTarget(publicRoomId, sentenceId));
        var shared = command(() -> sharingWorkflow.share(new MemberId(memberId),
                target.spaceId(), target.contentId(), target.locationId(), content.value()));
        return CommentResponse.of(shared.comment(), shared.author());
    }

    @Transactional(readOnly = true)
    public NewCommentCountResponse countNewPublicRoomComments(Long memberId, Long publicRoomId,
                                                               Long currentPassageId) {
        long count = query(() -> queryWorkflow.countNewPublicRoomComments(
                new MemberId(memberId), publicRoomId, currentPassageId));
        return new NewCommentCountResponse(count);
    }

    @Transactional(readOnly = true)
    public CommentedSentencesResponse findPublicRoomCommentedSentences(Long memberId, Long publicRoomId,
                                                                       Long currentPassageId) {
        return discoveryResponse(query(() -> queryWorkflow.findPublicRoomDiscovery(
                new MemberId(memberId), publicRoomId, currentPassageId)));
    }

    @Transactional(readOnly = true)
    public NewCommentCountResponse countNewComments(Long memberId, Long clubId, Long currentPassageId) {
        log.atInfo().addKeyValue(OPERATION, "comment.countNewComments")
                .addKeyValue(CLUB_ID, clubId).addKeyValue(CURRENT_PASSAGE_ID, currentPassageId)
                .log("새 댓글 수를 조회합니다.");
        long count = query(() -> queryWorkflow.countNewClubComments(
                new MemberId(memberId), clubId, currentPassageId));
        var response = new NewCommentCountResponse(count);
        log.atInfo().addKeyValue(OPERATION, "comment.countNewComments").addKeyValue(RESULT, SUCCESS)
                .addKeyValue(CLUB_ID, clubId).addKeyValue(CURRENT_PASSAGE_ID, currentPassageId)
                .addKeyValue("resultCount", count).log("새 댓글 수를 조회했습니다.");
        return response;
    }

    @Transactional(readOnly = true)
    public CommentedSentencesResponse findCommentedSentences(Long memberId, Long clubId, Long currentPassageId) {
        log.atInfo().addKeyValue(OPERATION, "comment.findCommentedSentences")
                .addKeyValue(CLUB_ID, clubId).addKeyValue(CURRENT_PASSAGE_ID, currentPassageId)
                .log("댓글이 있는 문장 목록을 조회합니다.");
        List<CommentedSentenceResponse> responses = discoveryResponse(query(() -> queryWorkflow.findClubDiscovery(
                new MemberId(memberId), clubId, currentPassageId))).commentedSentences();
        var response = new CommentedSentencesResponse(responses);
        log.atInfo().addKeyValue(OPERATION, "comment.findCommentedSentences").addKeyValue(RESULT, SUCCESS)
                .addKeyValue(CLUB_ID, clubId).addKeyValue(CURRENT_PASSAGE_ID, currentPassageId)
                .addKeyValue("resultCount", responses.size()).log("댓글이 있는 문장 목록을 조회했습니다.");
        return response;
    }

    @Transactional
    public CommentResponse update(Long memberId, Long commentId, CommentContent content) {
        log.atInfo().addKeyValue(OPERATION, "comment.update").addKeyValue(COMMENT_ID, commentId)
                .log("댓글을 수정합니다.");
        var changed = command(() -> modificationWorkflow.update(
                new MemberId(memberId), new AppreciationId(commentId), content.value()));
        var response = CommentResponse.of(changed.comment(), changed.author());
        log.atInfo().addKeyValue(OPERATION, "comment.update").addKeyValue(RESULT, SUCCESS)
                .addKeyValue(COMMENT_ID, commentId).log("댓글을 수정했습니다.");
        return response;
    }

    @Transactional
    public void delete(Long memberId, Long commentId) {
        log.atInfo().addKeyValue(OPERATION, "comment.delete").addKeyValue(COMMENT_ID, commentId)
                .log("댓글을 삭제합니다.");
        command(() -> {
            modificationWorkflow.delete(new MemberId(memberId), new AppreciationId(commentId));
            return null;
        });
        log.atInfo().addKeyValue(OPERATION, "comment.delete").addKeyValue(RESULT, SUCCESS)
                .addKeyValue(COMMENT_ID, commentId).log("댓글을 삭제했습니다.");
    }

    @Transactional
    public void report(Long memberId, Long commentId) {
        log.atInfo().addKeyValue(OPERATION, "comment.report").addKeyValue(COMMENT_ID, commentId)
                .log("댓글을 신고합니다.");
        boolean reportCreated = command(() -> reportWorkflow.report(
                new MemberId(memberId), new AppreciationId(commentId)));
        log.atInfo().addKeyValue(OPERATION, "comment.report").addKeyValue(RESULT, SUCCESS)
                .addKeyValue(COMMENT_ID, commentId)
                .addKeyValue("reportCreated", reportCreated)
                .log("댓글 작업을 완료했습니다.");
    }

    private CommentedSentenceResponse toResponse(DiscoveryResult summary) {
        return new CommentedSentenceResponse(
                summary.sentenceId(), summary.content(), summary.passageId(), summary.passageSequence(),
                summary.sentenceSequence(), summary.future(), summary.commentCount(), summary.unreadCommentCount(),
                summary.revealRequired() ? ContentVisibility.REVEAL_REQUIRED : ContentVisibility.VISIBLE,
                summary.latestCommentCreatedAt());
    }

    private CommentsResponse commentsResponse(Long requesterId, List<CommentResult> results) {
        return new CommentsResponse(results.stream().map(result -> new CommentResponse(
                result.comment().id().value(), result.author().id().value(), result.author().nickname(),
                result.comment().content(), result.comment().createdAt(), result.comment().updatedAt(),
                result.comment().isWrittenBy(new MemberId(requesterId)))).toList());
    }

    private CommentedSentencesResponse discoveryResponse(List<DiscoveryResult> results) {
        return new CommentedSentencesResponse(results.stream().map(this::toResponse).toList());
    }

    private <T> T query(QueryOperation<T> operation) {
        try {
            return operation.execute();
        } catch (CommentQueryFailure failure) {
            throw compatibleFailure(failure);
        }
    }

    private <T> T command(CommandOperation<T> operation) {
        try {
            return operation.execute();
        } catch (CommentFailure failure) {
            throw compatibleFailure(failure);
        } catch (CommentPolicyFailure failure) {
            throw compatibleFailure(failure);
        } catch (ContentUnavailableFailure failure) {
            throw new BadRequestException(ErrorCode.BOOK_NOT_AVAILABLE, failure.getMessage(), failure);
        } catch (ContentNotFoundFailure failure) {
            throw new BadRequestException(ErrorCode.BOOK_NOT_FOUND, failure.getMessage(), failure);
        }
    }

    private RuntimeException compatibleFailure(CommentQueryFailure failure) {
        return switch (failure.reason()) {
            case CLUB_NOT_FOUND -> new NotFoundException(
                    ErrorCode.CLUB_NOT_FOUND, failure.getMessage(), failure.context());
            case PUBLIC_ROOM_NOT_FOUND -> new NotFoundException(
                    ErrorCode.PUBLIC_ROOM_NOT_FOUND, failure.getMessage(), failure.context());
            case NOT_CLUB_MEMBER -> new ForbiddenException(
                    ErrorCode.NOT_CLUB_MEMBER, failure.getMessage(), failure.context());
            case SENTENCE_NOT_FOUND -> new NotFoundException(
                    ErrorCode.SENTENCE_NOT_FOUND, failure.getMessage(), failure.context());
            case SENTENCE_NOT_IN_CLUB_CONTENT -> new NotFoundException(
                    ErrorCode.SENTENCE_NOT_IN_CLUB_BOOK, failure.getMessage(), failure.context());
            case INVALID_REQUEST -> new BadRequestException(
                    ErrorCode.INVALID_REQUEST, failure.getMessage(), failure.context());
            case BOOK_NOT_AVAILABLE -> new BadRequestException(
                    ErrorCode.BOOK_NOT_AVAILABLE, failure.getMessage(), failure.context());
            case SPACE_ACCESS_DENIED -> new ForbiddenException(
                    ErrorCode.NOT_CLUB_MEMBER, failure.getMessage(), failure.context());
            case CONTENT_UNAVAILABLE -> new BadRequestException(
                    ErrorCode.BOOK_NOT_AVAILABLE, failure.getMessage(), failure.context());
        };
    }

    private RuntimeException compatibleFailure(CommentFailure failure) {
        Map<String, String> context = commentContext(failure.commentId());
        return switch (failure.reason()) {
            case NOT_FOUND, NOT_VISIBLE -> new NotFoundException(
                    ErrorCode.COMMENT_NOT_FOUND, failure.getMessage(), context);
            case NOT_OWNER -> new ForbiddenException(
                    ErrorCode.NOT_COMMENT_OWNER, failure.getMessage(), context);
            case CANNOT_REPORT_OWN_COMMENT -> new BadRequestException(
                    ErrorCode.CANNOT_REPORT_OWN_COMMENT, failure.getMessage(), context);
        };
    }

    private RuntimeException compatibleFailure(CommentPolicyFailure failure) {
        if (failure.reason() == CommentPolicyFailure.Reason.SPACE_ACCESS_DENIED) {
            return new ForbiddenException(ErrorCode.NOT_CLUB_MEMBER, failure.getMessage());
        }
        return new BadRequestException(ErrorCode.INVALID_REQUEST, failure.getMessage());
    }

    @FunctionalInterface
    private interface QueryOperation<T> {

        T execute();
    }

    @FunctionalInterface
    private interface CommandOperation<T> {

        T execute();
    }

    private Map<String, String> commentContext(Long commentId) {
        return Map.of(COMMENT_ID, commentId.toString());
    }
}
