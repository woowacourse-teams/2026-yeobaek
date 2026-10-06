package yeobaek.backend.content.api.book;

import java.util.List;
import yeobaek.backend.content.api.value.AuthorName;
import yeobaek.backend.content.api.value.BookTitle;
import yeobaek.backend.content.api.value.ChapterTitle;
import yeobaek.backend.content.api.value.Isni;
import yeobaek.backend.content.api.value.Publisher;
import yeobaek.backend.content.api.value.SentenceContent;
import yeobaek.backend.shared.identity.ContentId;

public interface BookIngestApi {

    Result ingest(Command command);

    record Command(BookTitle title, Publisher publisher, Integer publishedYear, String coverImageKey,
                   List<AuthorReference> authors, List<ChapterInput> chapters) {

        public Command {
            authors = List.copyOf(java.util.Objects.requireNonNullElse(authors, List.of()));
            chapters = List.copyOf(java.util.Objects.requireNonNullElse(chapters, List.of()));
        }
    }

    record AuthorReference(Long authorId, AuthorName name, Isni isni) {

        public boolean referencesExisting() {
            return authorId != null;
        }
    }

    record ChapterInput(ChapterTitle title, List<PassageInput> passages) {

        public ChapterInput {
            passages = List.copyOf(java.util.Objects.requireNonNullElse(passages, List.of()));
        }
    }

    record PassageInput(List<SentenceInput> sentences) {

        public PassageInput {
            sentences = List.copyOf(java.util.Objects.requireNonNullElse(sentences, List.of()));
        }
    }

    record SentenceInput(SentenceContent content) {
    }

    record Result(Long bookId, ContentId contentId, String title, String coverImageUrl, int passageCount) {
    }
}
