package yeobaek.backend.content.internal.metadata;

import java.util.List;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.content.api.metadata.ContentMetadataDetailResponse;
import yeobaek.backend.content.api.metadata.ContentMetadataResponse;
import yeobaek.backend.shared.identity.ContentId;

public interface ContentMetadataProvider {

    ContentKind supportedKind();

    List<? extends ContentMetadataResponse> search(String keyword);

    ContentMetadataDetailResponse get(ContentId contentId);
}
