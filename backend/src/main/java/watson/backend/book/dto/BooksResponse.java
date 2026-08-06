package watson.backend.book.dto;

import java.util.List;

public record BooksResponse(List<BookSummaryResponse> books) {
}
