package watson.backend.comment.dto;

import java.util.List;

public record CommentsResponse(List<CommentResponse> comments) {
}
