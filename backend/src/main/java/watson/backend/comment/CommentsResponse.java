package watson.backend.comment;

import java.util.List;

public record CommentsResponse(List<CommentResponse> comments) {
}
