package yeobaek.backend.content.book.internal.metadata;

import yeobaek.backend.content.api.metadata.ContentMetadataResponse.Creator;

public record BookAuthorResponse(String name) implements Creator {
}
