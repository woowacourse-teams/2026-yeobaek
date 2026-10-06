package yeobaek.backend.space.internal;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.space.api.access.SpaceAccessApi;
import yeobaek.backend.space.api.SpaceNotFoundException;
import yeobaek.backend.space.api.Space;
import yeobaek.backend.space.api.SpaceKind;
import yeobaek.backend.space.internal.access.SpaceAccessCapability;
import yeobaek.backend.space.persistence.SpaceRootRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpaceAccessService implements SpaceAccessApi {

    private final Map<SpaceKind, SpaceAccessCapability> capabilities;
    private final SpaceRootRepository spaces;

    @Override
    public Space getSpace(SpaceId spaceId) {
        SpaceKind canonicalKind = findKind(spaceId);
        return capability(canonicalKind).getSpace(spaceId);
    }

    @Override
    public boolean canAccess(MemberId memberId, SpaceId spaceId) {
        Space space = getSpace(spaceId);
        return capability(space.kind()).canAccess(memberId, space.id());
    }

    private SpaceKind findKind(SpaceId spaceId) {
        return spaces.findById(spaceId.value())
                .orElseThrow(() -> new SpaceNotFoundException(spaceId,
                        "접근 여부를 확인할 공간이 존재하지 않습니다: spaceId=" + spaceId.value()))
                .spaceKind();
    }

    private SpaceAccessCapability capability(SpaceKind kind) {
        SpaceAccessCapability capability = capabilities.get(kind);
        if (capability == null) {
            throw new IllegalArgumentException("지원하지 않는 capability입니다: " + kind);
        }
        return capability;
    }
}
