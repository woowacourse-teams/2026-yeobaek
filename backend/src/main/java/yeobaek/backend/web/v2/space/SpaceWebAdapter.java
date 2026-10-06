package yeobaek.backend.web.v2.space;

import java.util.List;
import yeobaek.backend.application.space.query.SpaceQueryResult;
import yeobaek.backend.application.reading.ReadingActivityResult;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.space.api.SpaceKind;

public interface SpaceWebAdapter {

    SpaceKind kind();

    SpaceResponses.Space create(MemberId actorId, SpaceRequests.CreateData data);

    SpaceResponses.Space join(MemberId actorId, SpaceRequests.JoinData data);

    List<SpaceQueryResult> findPublic(MemberId actorId, String sort);

    SpaceResponses.Space map(SpaceQueryResult result);

    yeobaek.backend.web.v2.reading.ReadingResponses.SpaceData mapReading(ReadingActivityResult.ReadingSpace space);
}
