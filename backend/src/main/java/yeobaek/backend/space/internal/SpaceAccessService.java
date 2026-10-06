package yeobaek.backend.space.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.space.api.access.SpaceAccessApi;
import yeobaek.backend.space.spi.access.SpaceAccessRegistry;
import yeobaek.backend.space.api.SpaceNotFoundException;
import yeobaek.backend.space.api.Space;
import yeobaek.backend.space.api.SpaceKind;
import yeobaek.backend.space.persistence.SpaceRootRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpaceAccessService implements SpaceAccessApi {

    private final SpaceAccessRegistry capabilities;
    private final SpaceRootRepository spaces;

    @Override
    public Space getSpace(SpaceId spaceId) {
        SpaceKind canonicalKind = findKind(spaceId);
        return capabilities.get(canonicalKind).getSpace(spaceId);
    }

    @Override
    public boolean canAccess(MemberId memberId, SpaceId spaceId) {
        Space space = getSpace(spaceId);
        return capabilities.get(space.kind()).canAccess(memberId, space.id());
    }

    private SpaceKind findKind(SpaceId spaceId) {
        return spaces.findById(spaceId.value())
                .orElseThrow(() -> new SpaceNotFoundException(spaceId,
                        "접근 여부를 확인할 공간이 존재하지 않습니다: spaceId=" + spaceId.value()))
                .spaceKind();
    }
}
