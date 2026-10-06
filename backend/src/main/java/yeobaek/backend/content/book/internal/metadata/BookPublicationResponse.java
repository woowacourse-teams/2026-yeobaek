package yeobaek.backend.content.book.internal.metadata;

import yeobaek.backend.content.api.metadata.ContentMetadataResponse.Publication;

public record BookPublicationResponse(
        BookPublisherResponse publisher,
        Integer publishedYear
) implements Publication {
}
