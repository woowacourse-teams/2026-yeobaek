package yeobaek.backend.book.api;

@FunctionalInterface
public interface BookCoverApi {

    String resolve(String coverImageKey);
}
