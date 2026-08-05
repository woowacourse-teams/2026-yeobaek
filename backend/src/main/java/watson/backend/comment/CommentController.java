package watson.backend.comment;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import watson.backend.auth.AuthMember;

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
}
