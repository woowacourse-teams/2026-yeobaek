package yeobaek.backend.content.book.internal.metadata;

import yeobaek.backend.content.api.metadata.ContentMetadataResponse.Publisher;

public record BookPublisherResponse(String name) implements Publisher {
}
