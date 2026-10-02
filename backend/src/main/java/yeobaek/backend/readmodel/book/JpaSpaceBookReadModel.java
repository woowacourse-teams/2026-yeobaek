package yeobaek.backend.readmodel.book;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.BookStatus;
import yeobaek.backend.book.repository.AuthorBookRepository;
import yeobaek.backend.foundation.identity.ContentId;
import yeobaek.backend.foundation.identity.SpaceId;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JpaSpaceBookReadModel implements SpaceBookReadModel {

    private final EntityManager entityManager;
    private final AuthorBookRepository authorBookRepository;

    @Override
    public Optional<BookSnapshot> findBook(SpaceId spaceId) {
        return entityManager.createQuery("""
                select book from SpaceContentBindingJpaEntity binding
                join Book book on book.contentId = binding.contentId
                where binding.spaceId = :spaceId
                """, Book.class).setParameter("spaceId", spaceId.value()).getResultStream()
                .findFirst().map(this::toSnapshot);
    }

    private BookSnapshot toSnapshot(Book book) {
        List<String> authors = authorBookRepository.findAllWithAuthorByBookIdIn(List.of(book.getId())).stream()
                .map(authorBook -> authorBook.getAuthor().getName().value()).toList();
        return new BookSnapshot(book.getId(), new ContentId(book.getContentId()), book.getTitle().value(),
                authors, book.getCoverImageKey(), book.getPassageCount().value(), book.getStatus().name(),
                book.getStatus() == BookStatus.ACTIVE);
    }

    @Override
    public Optional<BookSnapshot> findByBookId(Long bookId) {
        return Optional.ofNullable(entityManager.find(Book.class, bookId)).map(this::toSnapshot);
    }
}
