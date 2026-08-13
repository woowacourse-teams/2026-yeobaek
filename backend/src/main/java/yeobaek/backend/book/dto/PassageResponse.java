package yeobaek.backend.book.dto;

public record PassageResponse(
        Long passageId,
        int sequence,
        Long chapterId,
        String content,
        long commentCount
) {
}
