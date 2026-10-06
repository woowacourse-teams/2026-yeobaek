package yeobaek.backend.web.v2.appreciation;

import java.util.List;
import yeobaek.backend.appreciation.api.AppreciationKind;
import yeobaek.backend.shared.identity.AppreciationId;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.ContentLocationId;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;

public interface AppreciationWebAdapter {

    AppreciationKind kind();

    List<AppreciationResponses.Appreciation> find(MemberId actorId, SpaceId spaceId, ContentId contentId,
                                                   ContentLocationId locationId);

    AppreciationResponses.Appreciation create(MemberId actorId, SpaceId spaceId, ContentId contentId,
                                                ContentLocationId locationId, AppreciationRequests.Data data);

    AppreciationResponses.Appreciation update(MemberId actorId, AppreciationId appreciationId,
                                                AppreciationRequests.Data data);
}
