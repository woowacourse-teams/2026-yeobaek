package yeobaek.backend.web.v2.content;

import yeobaek.backend.application.content.ContentCardResult;
import yeobaek.backend.application.content.ContentMetadataQueryService.Detail;
import yeobaek.backend.application.content.ContentMetadataQueryService.Summary;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.web.v2.common.ContentCardResponse;

public interface ContentWebAdapter {

    ContentKind kind();

    ContentResponses.SummaryResponse summary(Summary result);

    ContentResponses.DetailResponse detail(Detail result);

    ContentCardResponse card(ContentCardResult result);
}
