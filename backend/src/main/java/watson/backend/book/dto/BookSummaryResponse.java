package watson.backend.book.dto;

import java.util.List;
import watson.backend.book.domain.Book;

public record BookSummaryResponse(
        Long bookId,
        String title,
        List<String> authors,
        String publisher,
        Integer publishedYear,
        int passageCount
) {

    public BookSummaryResponse {
        authors = List.copyOf(authors);
    }

    public static BookSummaryResponse of(Book book, List<String> authors) {
        return new BookSummaryResponse(book.getId(), book.getTitle(), authors,
                book.getPublisher(), book.getPublishedYear(), book.getPassageCount());
    }
}
