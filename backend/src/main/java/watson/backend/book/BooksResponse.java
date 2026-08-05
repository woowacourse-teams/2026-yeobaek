package watson.backend.book;

import java.util.List;

public record BooksResponse(List<BookSummaryResponse> books) {
}
