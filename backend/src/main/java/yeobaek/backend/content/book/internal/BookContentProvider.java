package yeobaek.backend.content.book.internal;

import jakarta.persistence.EntityManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import yeobaek.backend.content.spi.ContentProvider;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.content.api.ContentNotFoundException;
import yeobaek.backend.shared.identity.ContentId;

@Component
@RequiredArgsConstructor
public class BookContentProvider implements ContentProvider {

    private final EntityManager entityManager;

    @Override
    public ContentKind supportedKind() {
        return ContentKind.BOOK;
    }

    @Override
    public BookContentSnapshot get(ContentId contentId) {
        List<yeobaek.backend.content.book.persistence.Book> books = entityManager.createQuery(
                        "select b from Book b where b.contentId = :contentId",
                        yeobaek.backend.content.book.persistence.Book.class)
                .setParameter("contentId", contentId.value()).getResultList();
        if (books.isEmpty()) {
            throw new ContentNotFoundException(contentId,
                    "본문을 조회할 도서 컨텐츠가 존재하지 않습니다: contentId=" + contentId.value());
        }
        var book = books.getFirst();
        return new BookContentSnapshot(contentId, book.getId(),
                book.getStatus() == yeobaek.backend.content.book.domain.BookStatus.ACTIVE,
                book.getPassageCount().value());
    }
}
