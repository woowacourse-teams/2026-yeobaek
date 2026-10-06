package yeobaek.backend.content.book.internal.metadata;

import yeobaek.backend.content.api.metadata.ContentMetadataResponse.Section;

public record BookChapterResponse(
        long id,
        String title,
        int sequence,
        int startUnitSequence,
        int endUnitSequence
) implements Section {
}
