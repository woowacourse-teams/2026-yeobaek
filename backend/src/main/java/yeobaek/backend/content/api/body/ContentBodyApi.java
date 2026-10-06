package yeobaek.backend.content.api.body;

import java.util.List;
import java.util.Optional;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.ContentLocationId;

public interface ContentBodyApi {

    List<Passage> findPassages(ContentId contentId, int from, int to);

    Optional<Passage> findPassage(ContentId contentId, long passageId);

    Optional<Passage> findPassageByLocation(ContentId contentId, ContentLocationId locationId);

    int passageCount(ContentId contentId);

    record Passage(long passageId, ContentId contentId, ContentLocationId locationId, int sequence,
                   long chapterId, List<Sentence> sentences) {

        public Passage {
            sentences = List.copyOf(sentences);
        }
    }

    record Sentence(long sentenceId, ContentLocationId locationId, int sequence, String content) {
    }
}
