package yeobaek.backend.application.content;

import java.util.List;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.shared.identity.ContentId;

public record ContentCardResult(ContentId contentId, ContentKind kind, String title, List<String> creators,
                                String coverImageUrl, int unitCount, boolean available) {

    public ContentCardResult {
        creators = List.copyOf(creators);
    }
}
