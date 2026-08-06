package watson.backend.comment.controller;

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
import watson.backend.auth.AuthMember;
import watson.backend.comment.dto.CommentCreateRequest;
import watson.backend.comment.dto.CommentResponse;
import watson.backend.comment.dto.CommentUpdateRequest;
import watson.backend.comment.dto.CommentsResponse;
import watson.backend.comment.service.CommentService;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/api/clubs/{clubId}/passages/{passageId}/comments")
    public CommentsResponse findComments(@AuthMember Long memberId,
                                         @PathVariable Long clubId,
                                         @PathVariable Long passageId) {
        return commentService.findComments(memberId, clubId, passageId);
    }

    @PostMapping("/api/clubs/{clubId}/passages/{passageId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse create(@AuthMember Long memberId,
                                  @PathVariable Long clubId,
                                  @PathVariable Long passageId,
                                  @RequestBody CommentCreateRequest request) {
        return commentService.create(memberId, clubId, passageId, request.content());
    }

    @PutMapping("/api/comments/{commentId}")
    public CommentResponse update(@AuthMember Long memberId,
                                  @PathVariable Long commentId,
                                  @RequestBody CommentUpdateRequest request) {
        return commentService.update(memberId, commentId, request.content());
    }

    @DeleteMapping("/api/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthMember Long memberId, @PathVariable Long commentId) {
        commentService.delete(memberId, commentId);
    }
}
