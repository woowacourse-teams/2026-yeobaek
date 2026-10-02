package yeobaek.backend.book.api;

import java.util.List;
import yeobaek.backend.foundation.identity.ContentId;

@FunctionalInterface
public interface BookIngestApi {

    Result ingest(Command command);

    record Command(String title, String publisher, Integer publishedYear, String coverImageKey,
                   List<AuthorReference> authors, List<ChapterInput> chapters) {

        public Command {
            authors = List.copyOf(java.util.Objects.requireNonNullElse(authors, List.of()));
            chapters = List.copyOf(java.util.Objects.requireNonNullElse(chapters, List.of()));
        }
    }

    record AuthorReference(Long authorId, String name, String isni) {

        public boolean referencesExisting() {
            return authorId != null;
        }
    }

    record ChapterInput(String title, List<PassageInput> passages) {

        public ChapterInput {
            passages = List.copyOf(java.util.Objects.requireNonNullElse(passages, List.of()));
        }
    }

    record PassageInput(List<SentenceInput> sentences) {

        public PassageInput {
            sentences = List.copyOf(java.util.Objects.requireNonNullElse(sentences, List.of()));
        }
    }

    record SentenceInput(String content) {
    }

    record Result(Long bookId, ContentId contentId, String title, String coverImageKey, int passageCount) {
    }
}
