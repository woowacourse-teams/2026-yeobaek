package watson.backend.book;

public record PassageResponse(
        Long passageId,
        int sequence,
        Long chapterId,
        String content,
        String imageUrl,
        long commentCount
) {
}
