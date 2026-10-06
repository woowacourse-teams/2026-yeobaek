package yeobaek.backend.readmodel.book;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.collaboration.api.binding.SpaceContentBindingApi;
import yeobaek.backend.content.api.ContentApi;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.content.api.legacyreference.ContentLegacyReferenceApi;
import yeobaek.backend.content.api.legacyreference.ContentLegacyReferenceNotFoundException;
import yeobaek.backend.content.api.metadata.ContentMetadataApi;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.SpaceId;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultSpaceBookReadModel implements SpaceBookReadModel {

    private final SpaceContentBindingApi bindings;
    private final ContentApi contents;
    private final ContentMetadataApi metadata;
    private final ContentLegacyReferenceApi references;

    @Override
    public Optional<BookSnapshot> findBook(SpaceId spaceId) {
        return bindings.findContents(spaceId).stream()
                .filter(contentId -> ContentKind.BOOK.equals(contents.getContent(contentId).kind()))
                .findFirst().map(this::snapshot);
    }

    @Override
    public Optional<BookSnapshot> findByBookId(Long bookId) {
        try {
            return Optional.of(snapshot(references.resolve(ContentKind.BOOK, bookId)));
        } catch (ContentLegacyReferenceNotFoundException failure) {
            return Optional.empty();
        }
    }

    private BookSnapshot snapshot(ContentId contentId) {
        var detail = metadata.get(contentId);
        boolean available = contents.getContent(contentId).available();
        Long bookId = references.legacyIds(ContentKind.BOOK, List.of(contentId)).get(contentId);
        return new BookSnapshot(bookId, contentId, detail.title(),
                detail.creators().stream().map(creator -> creator.name()).toList(), detail.coverImageUrl(),
                detail.unitCount(), available ? "ACTIVE" : "DELETED", available);
    }
}
