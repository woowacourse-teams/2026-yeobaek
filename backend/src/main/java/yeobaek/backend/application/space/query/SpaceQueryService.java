package yeobaek.backend.application.space.query;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.space.api.SpaceKind;
import yeobaek.backend.space.api.access.SpaceAccessApi;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpaceQueryService {

    private final SpaceQueryRegistry providers;
    private final SpaceAccessApi spaces;

    public List<SpaceQueryResult> findMine(MemberId actorId, SpaceKind kind) {
        return List.copyOf(providers.get(kind).findMine(actorId));
    }

    public SpaceQueryResult findDetail(MemberId actorId, SpaceId spaceId) {
        return providers.get(spaces.getSpace(spaceId).kind()).findDetail(actorId, spaceId);
    }

    public List<SpaceQueryResult> findPublic(MemberId actorId, SpaceKind kind) {
        return List.copyOf(providers.getPublic(kind).findPublic(actorId));
    }
}
