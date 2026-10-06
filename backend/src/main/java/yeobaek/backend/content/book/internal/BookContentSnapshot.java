package yeobaek.backend.content.book.internal;

import yeobaek.backend.content.api.Content;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.shared.identity.ContentId;

public record BookContentSnapshot(
        ContentId id,
        long bookId,
        boolean available,
        int passageCount
) implements Content {

    @Override
    public ContentKind kind() {
        return ContentKind.BOOK;
    }
}
