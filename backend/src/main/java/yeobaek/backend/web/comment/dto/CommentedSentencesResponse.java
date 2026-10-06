package yeobaek.backend.web.comment.dto;

import java.util.List;

public record CommentedSentencesResponse(
        List<CommentedSentenceResponse> commentedSentences
) {

    public CommentedSentencesResponse {
        commentedSentences = List.copyOf(commentedSentences);
    }
}
