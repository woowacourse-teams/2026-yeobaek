package watson.backend.book;

public record ChapterResponse(
        Long chapterId,
        String title,
        int sequence,
        int startPassageSequence,
        int endPassageSequence
) {
}
