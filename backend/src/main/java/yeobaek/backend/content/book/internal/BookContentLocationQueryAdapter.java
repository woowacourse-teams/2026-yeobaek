package yeobaek.backend.content.book.internal;

import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.content.api.location.ContentLegacyLocationQueryApi;
import yeobaek.backend.content.api.location.LocationKind;
import yeobaek.backend.content.book.domain.Passage;
import yeobaek.backend.content.book.domain.Sentence;
import yeobaek.backend.content.spi.location.ContentLocationProvider;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.ContentLocationId;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookContentLocationQueryAdapter implements ContentLegacyLocationQueryApi, ContentLocationProvider {

    private final EntityManager entityManager;

    @Override
    public ContentKind supportedKind() {
        return ContentKind.BOOK;
    }

    @Override
    public Optional<PassageContext> findPassageContext(ContentLocationId locationId, LocationKind locationKind) {
        if (LocationKind.PASSAGE.equals(locationKind)) {
            return findPassageByLocation(locationId).map(this::passageContext);
        }
        if (LocationKind.SENTENCE.equals(locationKind)) {
            return findSentenceByLocation(locationId).map(sentence -> passageContext(sentence.getPassage()));
        }
        return Optional.empty();
    }

    @Override
    public Optional<LocationReference> findSentence(long sentenceId) {
        return entityManager.createQuery("select sentence from Sentence sentence where sentence.id = :sentenceId",
                        Sentence.class)
                .setParameter("sentenceId", sentenceId)
                .getResultStream()
                .findFirst()
                .map(this::reference);
    }

    @Override
    public Optional<LocationReference> findPassage(long passageId) {
        return entityManager.createQuery("select passage from Passage passage where passage.id = :passageId",
                        Passage.class)
                .setParameter("passageId", passageId)
                .getResultStream()
                .findFirst()
                .map(this::reference);
    }

    @Override
    public Optional<LocationReference> findByLocation(ContentLocationId locationId) {
        Optional<LocationReference> passage = findPassageByLocation(locationId).map(this::reference);
        if (passage.isPresent()) {
            return passage;
        }
        return findSentenceByLocation(locationId).map(this::reference);
    }

    @Override
    public List<SentenceInfo> findSentenceInfo(Collection<ContentLocationId> locationIds) {
        if (locationIds.isEmpty()) {
            return List.of();
        }
        List<Long> values = locationIds.stream().map(ContentLocationId::value).toList();
        return entityManager.createQuery("""
                        select sentence from Sentence sentence
                        where sentence.locationId in :locationIds
                        """, Sentence.class)
                .setParameter("locationIds", values)
                .getResultStream()
                .map(this::sentenceInfo)
                .toList();
    }

    private LocationReference reference(Sentence sentence) {
        Passage passage = sentence.getPassage();
        return new LocationReference(new ContentLocationId(sentence.getLocationId()),
                new ContentId(passage.getBook().getContentId()), passage.getId(),
                passage.getSequence().value());
    }

    private LocationReference reference(Passage passage) {
        return new LocationReference(new ContentLocationId(passage.getLocationId()),
                new ContentId(passage.getBook().getContentId()), passage.getId(),
                passage.getSequence().value());
    }

    private SentenceInfo sentenceInfo(Sentence sentence) {
        Passage passage = sentence.getPassage();
        return new SentenceInfo(new ContentLocationId(sentence.getLocationId()), sentence.getId(),
                sentence.getContent(), passage.getId(), passage.getSequence().value(),
                sentence.getSequence().value());
    }

    private Optional<Passage> findPassageByLocation(ContentLocationId locationId) {
        return entityManager.createQuery("""
                        select passage from Passage passage where passage.locationId = :locationId
                        """, Passage.class)
                .setParameter("locationId", locationId.value())
                .getResultStream()
                .findFirst();
    }

    private Optional<Sentence> findSentenceByLocation(ContentLocationId locationId) {
        return entityManager.createQuery("""
                        select sentence from Sentence sentence where sentence.locationId = :locationId
                        """, Sentence.class)
                .setParameter("locationId", locationId.value())
                .getResultStream()
                .findFirst();
    }

    private PassageContext passageContext(Passage passage) {
        return new PassageContext(new ContentLocationId(passage.getLocationId()), passage.getSequence().value());
    }
}
