package watson.backend.book;

import java.util.List;

public record BookDetailResponse(
        Long bookId,
        String title,
        List<String> authors,
        String publisher,
        Integer publishedYear,
        int passageCount,
        List<ChapterResponse> chapters
) {
}
