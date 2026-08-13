package yeobaek.backend.comment.dto;

import java.util.List;

public record CommentsResponse(List<CommentResponse> comments) {

    public CommentsResponse {
        comments = List.copyOf(comments);
    }
}
