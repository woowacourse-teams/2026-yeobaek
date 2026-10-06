package yeobaek.backend.content.book.internal.metadata;

import java.util.List;
import yeobaek.backend.content.api.metadata.ContentMetadataDetailResponse;
import yeobaek.backend.shared.identity.ContentId;

public record BookMetadataDetailResponse(
        ContentId contentId,
        String title,
        List<BookAuthorResponse> creators,
        BookPublicationResponse publication,
        String coverImageUrl,
        int unitCount,
        List<BookChapterResponse> sections
) implements ContentMetadataDetailResponse {

    public BookMetadataDetailResponse {
        creators = List.copyOf(creators);
        sections = List.copyOf(sections);
    }
}
