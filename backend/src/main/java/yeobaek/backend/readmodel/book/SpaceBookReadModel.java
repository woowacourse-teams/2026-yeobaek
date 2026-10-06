package yeobaek.backend.readmodel.book;

import java.util.List;
import java.util.Optional;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.SpaceId;

public interface SpaceBookReadModel {

    Optional<BookSnapshot> findBook(SpaceId spaceId);

    Optional<BookSnapshot> findByBookId(Long bookId);

    record BookSnapshot(Long bookId, ContentId contentId, String title, List<String> authors,
                        String coverImageUrl, int passageCount, String status, boolean available) {

        public BookSnapshot {
            authors = List.copyOf(authors);
        }
    }
}
