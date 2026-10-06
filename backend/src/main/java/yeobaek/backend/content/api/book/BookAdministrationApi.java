package yeobaek.backend.content.api.book;

import java.util.List;

public interface BookAdministrationApi {

    List<BookView> findBooks();

    List<AuthorView> findAuthors();

    List<BookStatusView> findBookStatuses();

    void delete(long bookId);

    void replaceCoverImage(long bookId, String coverImageKey);

    void removeCoverImage(long bookId);

    record AuthorSummary(long authorId, String name, String isni) {
    }

    record BookView(
            long bookId,
            String title,
            List<AuthorSummary> authors,
            String publisher,
            Integer publishedYear,
            int unitCount,
            String coverImageUrl,
            String status
    ) {

        public BookView {
            authors = List.copyOf(authors);
        }
    }

    record AuthorBookSummary(long bookId, String title, String coverImageUrl, String status) {
    }

    record AuthorView(long authorId, String name, String isni, List<AuthorBookSummary> books) {

        public AuthorView {
            books = List.copyOf(books);
        }
    }

    record BookStatusView(long bookId, String title, String status) {
    }
}
