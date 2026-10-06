package yeobaek.backend.content.book.internal;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.content.book.domain.Passage;
import yeobaek.backend.content.book.repository.PassageRepository;
import yeobaek.backend.content.api.body.ContentBodyApi;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.content.spi.body.ContentBodyProvider;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.ContentLocationId;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookContentBodyService implements ContentBodyProvider {

    private final BookContentProvider contentProvider;
    private final PassageRepository passageRepository;

    @Override
    public ContentKind supportedKind() {
        return ContentKind.BOOK;
    }

    @Override
    public List<ContentBodyApi.Passage> findPassages(ContentId contentId, int from, int to) {
        BookContentSnapshot book = contentProvider.get(contentId);
        return passageRepository.findRangeByBookId(book.bookId(), from, to).stream()
                .map(this::toBody).toList();
    }

    @Override
    public Optional<ContentBodyApi.Passage> findPassage(long passageId) {
        return passageRepository.findById(passageId).map(this::toBody);
    }

    @Override
    public Optional<ContentBodyApi.Passage> findPassageByLocation(ContentLocationId locationId) {
        return passageRepository.findByLocationId(locationId.value()).map(this::toBody);
    }

    @Override
    public int passageCount(ContentId contentId) {
        return contentProvider.get(contentId).passageCount();
    }

    private ContentBodyApi.Passage toBody(Passage passage) {
        return new ContentBodyApi.Passage(passage.getId(),
                new ContentId(passage.getBook().getContentId()),
                new ContentLocationId(passage.getLocationId()),
                passage.getSequence().value(), passage.getChapter().getId(), passage.getSentences().stream()
                .map(sentence -> new ContentBodyApi.Sentence(sentence.getId(),
                        new ContentLocationId(sentence.getLocationId()), sentence.getSequence().value(),
                        sentence.getContent()))
                .toList());
    }
}
