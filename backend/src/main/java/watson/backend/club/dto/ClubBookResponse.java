package watson.backend.club.dto;

import java.util.List;
import watson.backend.book.domain.Book;

public record ClubBookResponse(Long bookId, String title, List<String> authors, int passageCount) {

    public static ClubBookResponse of(Book book, List<String> authors) {
        return new ClubBookResponse(book.getId(), book.getTitle(), authors, book.getPassageCount());
    }
}
