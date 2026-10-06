package yeobaek.backend.content.book.internal.metadata;

import java.util.List;
import yeobaek.backend.content.api.metadata.ContentMetadataResponse;
import yeobaek.backend.shared.identity.ContentId;

public record BookMetadataResponse(
        ContentId contentId,
        String title,
        List<BookAuthorResponse> creators,
        BookPublicationResponse publication,
        String coverImageUrl,
        int unitCount
) implements ContentMetadataResponse {

    public BookMetadataResponse {
        creators = List.copyOf(creators);
    }
}
