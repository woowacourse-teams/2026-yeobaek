package yeobaek.backend.comment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import yeobaek.backend.auth.AuthMember;
import yeobaek.backend.comment.dto.CommentCreateRequest;
import yeobaek.backend.comment.dto.CommentResponse;
import yeobaek.backend.comment.dto.CommentUpdateRequest;
import yeobaek.backend.comment.dto.CommentsResponse;
import yeobaek.backend.comment.service.CommentService;

@Tag(name = "댓글")
@SecurityRequirement(name = "memberId")
@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "문장의 댓글 목록 조회", description = "작성일 오름차순. 이 모임에서 작성된 댓글만 보인다.")
    @GetMapping("/api/clubs/{clubId}/sentences/{sentenceId}/comments")
    public CommentsResponse findComments(@AuthMember Long memberId,
                                         @Parameter(description = "모임 ID") @PathVariable Long clubId,
                                         @Parameter(description = "문장 ID") @PathVariable Long sentenceId) {
        return commentService.findComments(memberId, clubId, sentenceId);
    }

    @Operation(summary = "댓글 작성")
    @PostMapping("/api/clubs/{clubId}/sentences/{sentenceId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse create(@AuthMember Long memberId,
                                  @Parameter(description = "모임 ID") @PathVariable Long clubId,
                                  @Parameter(description = "문장 ID") @PathVariable Long sentenceId,
                                  @RequestBody CommentCreateRequest request) {
        return commentService.create(memberId, clubId, sentenceId, request.content());
    }

    @Operation(summary = "댓글 수정", description = "본인 댓글이 아니면 403.")
    @PutMapping("/api/comments/{commentId}")
    public CommentResponse update(@AuthMember Long memberId,
                                  @Parameter(description = "댓글 ID") @PathVariable Long commentId,
                                  @RequestBody CommentUpdateRequest request) {
        return commentService.update(memberId, commentId, request.content());
    }

    @Operation(summary = "댓글 삭제", description = "하드 삭제(PRD 3.5). 본인 댓글이 아니면 403.")
    @DeleteMapping("/api/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthMember Long memberId, @Parameter(description = "댓글 ID") @PathVariable Long commentId) {
        commentService.delete(memberId, commentId);
    }
}
