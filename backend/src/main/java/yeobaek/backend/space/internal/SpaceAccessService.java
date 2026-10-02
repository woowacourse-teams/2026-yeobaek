package yeobaek.backend.space.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.foundation.identity.SpaceId;
import yeobaek.backend.space.api.SpaceAccessApi;
import yeobaek.backend.space.api.SpaceAccessRegistry;
import yeobaek.backend.space.api.SpaceNotFoundFailure;
import yeobaek.backend.space.domain.Space;
import yeobaek.backend.space.persistence.SpaceRootRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpaceAccessService implements SpaceAccessApi {

    private final SpaceAccessRegistry capabilities;
    private final SpaceRootRepository spaces;

    @Override
    public Space getSpace(SpaceId spaceId) {
        String canonicalKind = findKind(spaceId);
        return capabilities.get(canonicalKind).getSpace(spaceId);
    }

    @Override
    public boolean canAccess(MemberId memberId, SpaceId spaceId) {
        Space space = getSpace(spaceId);
        return capabilities.get(space.kind()).canAccess(memberId, space.id());
    }

    private String findKind(SpaceId spaceId) {
        return spaces.findById(spaceId.value())
                .orElseThrow(() -> new SpaceNotFoundFailure(spaceId))
                .getKind();
    }
}
