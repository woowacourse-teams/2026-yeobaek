package yeobaek.backend.book.dto;

public record ChapterResponse(
        Long chapterId,
        String title,
        int sequence,
        int startPassageSequence,
        int endPassageSequence
) {
}
