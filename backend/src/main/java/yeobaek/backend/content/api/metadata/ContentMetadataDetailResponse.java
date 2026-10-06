package yeobaek.backend.content.api.metadata;

import java.util.List;

public interface ContentMetadataDetailResponse extends ContentMetadataResponse {

    List<? extends Section> sections();
}
