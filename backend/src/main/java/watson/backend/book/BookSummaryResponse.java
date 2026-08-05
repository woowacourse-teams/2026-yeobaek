package watson.backend.book;

import java.util.List;

public record BookSummaryResponse(
        Long bookId,
        String title,
        List<String> authors,
        String publisher,
        Integer publishedYear,
        int passageCount
) {

    public static BookSummaryResponse of(Book book, List<String> authors) {
        return new BookSummaryResponse(book.getId(), book.getTitle(), authors,
                book.getPublisher(), book.getPublishedYear(), book.getPassageCount());
    }
}
