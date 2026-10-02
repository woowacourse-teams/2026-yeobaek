package yeobaek.backend.comment.controller;

import static yeobaek.backend.support.LogField.CLUB_ID;
import static yeobaek.backend.support.LogField.COMMENT_ID;
import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.RESULT;
import static yeobaek.backend.support.LogField.SUCCESS;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import yeobaek.backend.auth.AuthMember;
import yeobaek.backend.comment.dto.CommentCreateRequest;
import yeobaek.backend.comment.dto.CommentedSentencesResponse;
import yeobaek.backend.comment.dto.CommentResponse;
import yeobaek.backend.comment.dto.CommentUpdateRequest;
import yeobaek.backend.comment.dto.CommentsResponse;
import yeobaek.backend.comment.dto.NewCommentCountResponse;
import yeobaek.backend.web.compatibility.CommentService;
import yeobaek.backend.support.analytics.AnalyticsEvent;
import yeobaek.backend.support.analytics.AnalyticsTracker;

@Tag(name = "댓글")
@SecurityRequirement(name = "memberId")
@RestController
@RequiredArgsConstructor
@Slf4j
public class CommentController {

    private static final String CLUB_ID_DESCRIPTION = "모임 ID";

    private final CommentService commentService;
    private final AnalyticsTracker analyticsTracker;

    @Operation(summary = "문장의 댓글 목록 조회 (deprecated)",
            description = "호환 API. 보이는 댓글을 작성일 오름차순으로 반환하고 VIEWED로 전환한다. 신규 클라이언트는 POST 상세 조회를 사용한다.",
            deprecated = true)
    @GetMapping("/api/clubs/{clubId}/sentences/{sentenceId}/comments")
    public CommentsResponse findComments(@AuthMember Long memberId,
                                         @Parameter(description = CLUB_ID_DESCRIPTION) @PathVariable Long clubId,
                                         @Parameter(description = "문장 ID") @PathVariable Long sentenceId) {
        logCommentAttempt("comment.findComments", clubId, sentenceId);
        CommentsResponse response = commentService.findComments(memberId, clubId, sentenceId);
        analyticsTracker.track(memberId, AnalyticsEvent.commentsViewFromDeprecatedGet(
                clubId, sentenceId, response.comments().size()));
        logCommentSuccess("comment.findComments", clubId, sentenceId, response.comments().size());
        return response;
    }

    @Operation(summary = "문장의 댓글 상세 조회와 직접 확인", description = "보이는 댓글을 작성일 오름차순으로 반환하고 VIEWED로 전환한다.")
    @PostMapping("/api/clubs/{clubId}/sentences/{sentenceId}/comment-detail-views")
    public CommentsResponse findCommentDetails(@AuthMember Long memberId,
                                               @Parameter(description = CLUB_ID_DESCRIPTION) @PathVariable Long clubId,
                                               @Parameter(description = "문장 ID") @PathVariable Long sentenceId) {
        logCommentAttempt("comment.findCommentDetails", clubId, sentenceId);
        CommentsResponse response = commentService.findComments(memberId, clubId, sentenceId);
        analyticsTracker.track(memberId, AnalyticsEvent.commentsViewFromExplicitPost(
                clubId, sentenceId, response.comments().size()));
        logCommentSuccess("comment.findCommentDetails", clubId, sentenceId, response.comments().size());
        return response;
    }

    @Operation(summary = "탑바 새 댓글 개수")
    @GetMapping("/api/clubs/{clubId}/comments/new-count")
    public NewCommentCountResponse countNewComments(
            @AuthMember Long memberId,
            @Parameter(description = CLUB_ID_DESCRIPTION) @PathVariable Long clubId,
            @RequestParam("currentPassageId") Long currentPassageId) {
        log.atInfo().addKeyValue(OPERATION, "comment.countNewComments")
                .addKeyValue(CLUB_ID, clubId)
                .addKeyValue("passageId", currentPassageId).log("새 댓글 수 API 처리를 시작합니다.");
        NewCommentCountResponse response = commentService.countNewComments(memberId, clubId, currentPassageId);
        analyticsTracker.track(memberId, AnalyticsEvent.newCommentCountView(
                clubId, currentPassageId, response.newCommentCount()));
        log.atInfo().addKeyValue(OPERATION, "comment.countNewComments").addKeyValue(RESULT, SUCCESS)
                .addKeyValue(CLUB_ID, clubId)
                .addKeyValue("resultCount", response.newCommentCount()).log("새 댓글 수 API 처리를 완료했습니다.");
        return response;
    }

    @Operation(summary = "댓글 문장 전체 목록 조회")
    @GetMapping("/api/clubs/{clubId}/commented-sentences")
    public CommentedSentencesResponse findCommentedSentences(
            @AuthMember Long memberId,
            @Parameter(description = CLUB_ID_DESCRIPTION) @PathVariable Long clubId,
            @RequestParam("currentPassageId") Long currentPassageId) {
        log.atInfo().addKeyValue(OPERATION, "comment.findCommentedSentences")
                .addKeyValue(CLUB_ID, clubId)
                .addKeyValue("passageId", currentPassageId).log("댓글 문장 목록 API 처리를 시작합니다.");
        CommentedSentencesResponse response = commentService.findCommentedSentences(
                memberId, clubId, currentPassageId);
        analyticsTracker.track(memberId, AnalyticsEvent.commentedSentencesView(
                clubId, currentPassageId, response.commentedSentences().size()));
        log.atInfo().addKeyValue(OPERATION, "comment.findCommentedSentences").addKeyValue(RESULT, SUCCESS)
                .addKeyValue(CLUB_ID, clubId)
                .addKeyValue("resultCount", response.commentedSentences().size()).log("댓글 문장 목록 API 처리를 완료했습니다.");
        return response;
    }

    @Operation(summary = "댓글 작성")
    @PostMapping("/api/clubs/{clubId}/sentences/{sentenceId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse create(@AuthMember Long memberId,
                                  @Parameter(description = CLUB_ID_DESCRIPTION) @PathVariable Long clubId,
                                  @Parameter(description = "문장 ID") @PathVariable Long sentenceId,
                                  @Valid @RequestBody CommentCreateRequest request) {
        logCommentAttempt("comment.create", clubId, sentenceId);
        CommentResponse response = commentService.create(memberId, clubId, sentenceId, request.content());
        analyticsTracker.track(memberId,
                AnalyticsEvent.commentCreate(clubId, sentenceId, response.commentId()));
        logCommentSuccess("comment.create", clubId, sentenceId, null);
        return response;
    }

    @Operation(summary = "댓글 수정", description = "본인 댓글이 아니면 403.")
    @PutMapping("/api/comments/{commentId}")
    public CommentResponse update(@AuthMember Long memberId,
                                  @Parameter(description = "댓글 ID") @PathVariable Long commentId,
                                  @Valid @RequestBody CommentUpdateRequest request) {
        log.atInfo().addKeyValue(OPERATION, "comment.update")
                .addKeyValue(COMMENT_ID, commentId).log("댓글 수정 API 처리를 시작합니다.");
        CommentResponse response = commentService.update(memberId, commentId, request.content());
        analyticsTracker.track(memberId, AnalyticsEvent.commentUpdate(response.commentId()));
        log.atInfo().addKeyValue(OPERATION, "comment.update").addKeyValue(RESULT, SUCCESS)
                .addKeyValue(COMMENT_ID, commentId).log("댓글 수정 API 처리를 완료했습니다.");
        return response;
    }

    @Operation(summary = "댓글 삭제", description = "하드 삭제(PRD 3.5). 본인 댓글이 아니면 403.")
    @DeleteMapping("/api/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthMember Long memberId, @Parameter(description = "댓글 ID") @PathVariable Long commentId) {
        logCommentMutationAttempt("comment.delete", commentId);
        commentService.delete(memberId, commentId);
        analyticsTracker.track(memberId, AnalyticsEvent.commentDelete(commentId));
        logCommentMutationSuccess("comment.delete", commentId);
    }

    @Operation(summary = "댓글 신고", description = "같은 회원의 동일 댓글 재신고는 새 신고를 만들지 않는다.")
    @PostMapping("/api/comments/{commentId}/reports")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void report(@AuthMember Long memberId, @Parameter(description = "댓글 ID") @PathVariable Long commentId) {
        logCommentMutationAttempt("comment.report", commentId);
        commentService.report(memberId, commentId);
        analyticsTracker.track(memberId, AnalyticsEvent.commentReport(commentId));
        logCommentMutationSuccess("comment.report", commentId);
    }

    private void logCommentAttempt(String operation, Long clubId, Long sentenceId) {
        log.atInfo().addKeyValue(OPERATION, operation)
                .addKeyValue(CLUB_ID, clubId).addKeyValue("sentenceId", sentenceId)
                .log("댓글 API 처리를 시작합니다.");
    }

    private void logCommentSuccess(String operation, Long clubId, Long sentenceId, Integer resultCount) {
        var event = log.atInfo().addKeyValue(OPERATION, operation).addKeyValue(RESULT, SUCCESS)
                .addKeyValue(CLUB_ID, clubId).addKeyValue("sentenceId", sentenceId);
        if (resultCount != null) {
            event = event.addKeyValue("resultCount", resultCount);
        }
        event.log("댓글 API 처리를 완료했습니다.");
    }

    private void logCommentMutationAttempt(String operation, Long commentId) {
        log.atInfo().addKeyValue(OPERATION, operation)
                .addKeyValue(COMMENT_ID, commentId).log("댓글 API 처리를 시작합니다.");
    }

    private void logCommentMutationSuccess(String operation, Long commentId) {
        log.atInfo().addKeyValue(OPERATION, operation).addKeyValue(RESULT, SUCCESS)
                .addKeyValue(COMMENT_ID, commentId).log("댓글 API 처리를 완료했습니다.");
    }
}
