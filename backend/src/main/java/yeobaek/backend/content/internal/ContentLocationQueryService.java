package yeobaek.backend.content.internal;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.content.api.location.ContentLocationNotFoundException;
import yeobaek.backend.content.api.location.ContentLocationQueryApi;
import yeobaek.backend.content.persistence.ContentLocationJpaEntity;
import yeobaek.backend.content.persistence.ContentLocationRepository;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.content.internal.location.ContentLocationProvider;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.ContentLocationId;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentLocationQueryService implements ContentLocationQueryApi {

    private final ContentLocationRepository locations;
    private final Map<ContentKind, ContentLocationProvider> providers;

    @Override
    public Location get(ContentLocationId locationId) {
        ContentLocationJpaEntity location = locations.findById(locationId.value())
                .orElseThrow(() -> new ContentLocationNotFoundException(locationId,
                        "컨텐츠 위치를 찾을 수 없습니다: locationId=" + locationId.value()));
        var passage = provider(location.getContent().contentKind())
                .findPassageContext(locationId, location.locationKind())
                .orElseThrow(() -> new ContentLocationNotFoundException(locationId,
                        "컨텐츠 위치 구현을 찾을 수 없습니다: locationId=" + locationId.value()));
        return new Location(locationId, new ContentId(location.getContent().getId()), location.locationKind(),
                passage.locationId(), passage.sequence());
    }

    private ContentLocationProvider provider(ContentKind kind) {
        ContentLocationProvider provider = providers.get(kind);
        if (provider == null) {
            throw new IllegalArgumentException("지원하지 않는 capability입니다: " + kind);
        }
        return provider;
    }
}
