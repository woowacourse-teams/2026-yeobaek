package yeobaek.backend.book.internal;

import jakarta.persistence.EntityManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import yeobaek.backend.content.api.ContentProvider;
import yeobaek.backend.content.api.ContentNotFoundFailure;
import yeobaek.backend.content.domain.Book;
import yeobaek.backend.foundation.identity.ContentId;

@Component
@RequiredArgsConstructor
public class BookContentProvider implements ContentProvider {

    private final EntityManager entityManager;

    @Override
    public String supportedKind() {
        return Book.KIND;
    }

    @Override
    public Book get(ContentId contentId) {
        List<yeobaek.backend.book.domain.Book> books = entityManager.createQuery(
                        "select b from Book b where b.contentId = :contentId",
                        yeobaek.backend.book.domain.Book.class)
                .setParameter("contentId", contentId.value()).getResultList();
        if (books.isEmpty()) {
            throw new ContentNotFoundFailure(contentId);
        }
        var book = books.getFirst();
        return new Book(contentId, book.getId(),
                book.getStatus() == yeobaek.backend.book.domain.BookStatus.ACTIVE,
                book.getPassageCount().value());
    }
}
