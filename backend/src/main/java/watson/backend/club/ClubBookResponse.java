package watson.backend.club;

import java.util.List;
import watson.backend.book.Book;

public record ClubBookResponse(Long bookId, String title, List<String> authors, int passageCount) {

    public static ClubBookResponse of(Book book, List<String> authors) {
        return new ClubBookResponse(book.getId(), book.getTitle(), authors, book.getPassageCount());
    }
}
