package yeobaek.backend.query.book;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.collaboration.api.binding.SpaceContentBindingApi;
import yeobaek.backend.content.api.ContentApi;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.content.api.idmapping.ContentIdMappingApi;
import yeobaek.backend.content.api.metadata.ContentMetadataApi;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.SpaceId;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpaceBookQueryService {

    private final SpaceContentBindingApi bindings;
    private final ContentApi contents;
    private final ContentMetadataApi metadata;
    private final ContentIdMappingApi idMappings;

    public Optional<BookSnapshot> findBook(SpaceId spaceId) {
        return bindings.findContents(spaceId).stream()
                .filter(contentId -> ContentKind.BOOK.equals(contents.getContent(contentId).kind()))
                .findFirst().map(this::snapshot);
    }

    private BookSnapshot snapshot(ContentId contentId) {
        var detail = metadata.get(contentId);
        boolean available = contents.getContent(contentId).available();
        Long bookId = idMappings.toImplementationIds(ContentKind.BOOK, List.of(contentId)).get(contentId);
        return new BookSnapshot(bookId, contentId, detail.title(),
                detail.creators().stream().map(creator -> creator.name()).toList(), detail.coverImageUrl(),
                detail.unitCount(), available ? "ACTIVE" : "DELETED", available);
    }

    public record BookSnapshot(Long bookId, ContentId contentId, String title, List<String> authors,
                               String coverImageUrl, int passageCount, String status, boolean available) {

        public BookSnapshot {
            authors = List.copyOf(authors);
        }
    }
}
