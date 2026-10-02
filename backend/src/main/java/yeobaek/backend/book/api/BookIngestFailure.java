package yeobaek.backend.book.api;

import java.util.Map;

public final class BookIngestFailure extends RuntimeException {

    private final Reason failureReason;
    private final Map<String, String> failureContext;

    public BookIngestFailure(Reason reason, String message, Map<String, String> context) {
        super(message);
        this.failureReason = reason;
        this.failureContext = Map.copyOf(context);
    }

    public Reason reason() {
        return failureReason;
    }

    public Map<String, String> context() {
        return failureContext;
    }

    public enum Reason {
        AUTHORS_EMPTY,
        CHAPTERS_EMPTY,
        PASSAGES_EMPTY,
        SENTENCES_EMPTY,
        SENTENCE_TOO_LARGE,
        MIXED_AUTHOR_REFERENCE,
        AUTHOR_NOT_FOUND,
        AUTHOR_NAME_MISMATCH,
        DUPLICATE_AUTHOR,
        DUPLICATE_AUTHOR_ISNI,
        DUPLICATE_BOOK
    }
}
