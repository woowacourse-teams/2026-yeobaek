package watson.backend.book.dto;

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

    public BookDetailResponse {
        authors = List.copyOf(authors);
        chapters = List.copyOf(chapters);
    }
}
