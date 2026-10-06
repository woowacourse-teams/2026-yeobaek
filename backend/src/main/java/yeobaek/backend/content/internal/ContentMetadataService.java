package yeobaek.backend.content.internal;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.content.api.metadata.ContentMetadataApi;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.content.api.metadata.ContentMetadataDetailResponse;
import yeobaek.backend.content.spi.metadata.ContentMetadataProviderRegistry;
import yeobaek.backend.content.api.metadata.ContentMetadataResponse;
import yeobaek.backend.content.api.ContentNotFoundException;
import yeobaek.backend.content.persistence.ContentRootRepository;
import yeobaek.backend.shared.identity.ContentId;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentMetadataService implements ContentMetadataApi {

    private final ContentRootRepository rootRepository;
    private final ContentMetadataProviderRegistry providers;

    @Override
    public List<ContentMetadataResponse> search(ContentKind kind, String keyword) {
        return List.copyOf(providers.get(kind).search(keyword));
    }

    @Override
    public ContentMetadataDetailResponse get(ContentId contentId) {
        ContentKind kind = rootRepository.findById(contentId.value())
                .orElseThrow(() -> new ContentNotFoundException(contentId,
                        "메타데이터를 조회할 컨텐츠가 존재하지 않습니다: contentId=" + contentId.value()))
                .contentKind();
        return providers.get(kind).get(contentId);
    }
}
