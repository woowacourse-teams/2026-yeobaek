package yeobaek.backend.web.v2.content;

import org.springframework.stereotype.Component;
import yeobaek.backend.application.content.ContentCardResult;
import yeobaek.backend.application.content.ContentMetadataQueryService.Detail;
import yeobaek.backend.application.content.ContentMetadataQueryService.Summary;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.web.v2.common.ContentCardResponse;

@Component
public class BookContentWebAdapter implements ContentWebAdapter {

    @Override
    public ContentKind kind() {
        return ContentKind.BOOK;
    }

    @Override
    public ContentResponses.SummaryResponse summary(Summary result) {
        return ContentResponses.book(result);
    }

    @Override
    public ContentResponses.DetailResponse detail(Detail result) {
        return ContentResponses.book(result);
    }

    @Override
    public ContentCardResponse card(ContentCardResult result) {
        var status = result.available() ? ContentCardResponse.Status.ACTIVE : ContentCardResponse.Status.DELETED;
        return new ContentCardResponse(result.contentId().value(), result.kind(),
                new ContentCardResponse.BookData(result.title(), result.creators(), result.coverImageUrl(),
                        result.unitCount(), status));
    }
}
