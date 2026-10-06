package yeobaek.backend.content.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.content.api.location.ContentLocationNotFoundException;
import yeobaek.backend.content.api.location.ContentLocationQueryApi;
import yeobaek.backend.content.persistence.ContentLocationJpaEntity;
import yeobaek.backend.content.persistence.ContentLocationRepository;
import yeobaek.backend.content.spi.location.ContentLocationProviderRegistry;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.ContentLocationId;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentLocationQueryService implements ContentLocationQueryApi {

    private final ContentLocationRepository locations;
    private final ContentLocationProviderRegistry providers;

    @Override
    public Location get(ContentLocationId locationId) {
        ContentLocationJpaEntity location = locations.findById(locationId.value())
                .orElseThrow(() -> new ContentLocationNotFoundException(locationId,
                        "컨텐츠 위치를 찾을 수 없습니다: locationId=" + locationId.value()));
        var passage = providers.get(location.getContent().contentKind())
                .findPassageContext(locationId, location.locationKind())
                .orElseThrow(() -> new ContentLocationNotFoundException(locationId,
                        "컨텐츠 위치 구현을 찾을 수 없습니다: locationId=" + locationId.value()));
        return new Location(locationId, new ContentId(location.getContent().getId()), location.locationKind(),
                passage.locationId(), passage.sequence());
    }
}
