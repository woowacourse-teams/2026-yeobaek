package yeobaek.backend.content.book.internal;

import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.content.api.idmapping.ContentIdMappingNotFoundException;
import yeobaek.backend.content.book.persistence.Book;
import yeobaek.backend.content.internal.idmapping.ContentIdMappingProvider;
import yeobaek.backend.shared.identity.ContentId;

@Component
@RequiredArgsConstructor
public class BookIdMappingAdapter implements ContentIdMappingProvider {

    private final EntityManager entityManager;

    @Override
    public ContentKind supportedKind() {
        return ContentKind.BOOK;
    }

    @Override
    public ContentId toContentId(long implementationId) {
        return entityManager.createQuery("""
                        select book.contentId from Book book where book.id = :legacyId
                        """, Long.class)
                .setParameter("legacyId", implementationId)
                .getResultStream()
                .findFirst()
                .map(ContentId::new)
                .orElseThrow(() -> new ContentIdMappingNotFoundException(ContentKind.BOOK, implementationId,
                        "레거시 도서 식별자에 해당하는 컨텐츠가 존재하지 않습니다: legacyId=" + implementationId));
    }

    @Override
    public Map<ContentId, Long> toImplementationIds(Collection<ContentId> contentIds) {
        if (contentIds.isEmpty()) {
            return Map.of();
        }
        List<Long> values = contentIds.stream().map(ContentId::value).toList();
        return entityManager.createQuery("""
                        select book from Book book where book.contentId in :contentIds
                        """, Book.class)
                .setParameter("contentIds", values)
                .getResultStream()
                .collect(Collectors.toUnmodifiableMap(
                        book -> new ContentId(book.getContentId()),
                        Book::getId));
    }
}
