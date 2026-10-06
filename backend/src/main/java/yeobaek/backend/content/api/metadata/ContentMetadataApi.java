package yeobaek.backend.content.api.metadata;

import java.util.List;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.shared.identity.ContentId;

public interface ContentMetadataApi {

    List<ContentMetadataResponse> search(ContentKind kind, String keyword);

    ContentMetadataDetailResponse get(ContentId contentId);
}
