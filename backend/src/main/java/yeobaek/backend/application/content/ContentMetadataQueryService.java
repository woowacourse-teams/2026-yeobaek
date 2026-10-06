package yeobaek.backend.application.content;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.content.api.ContentApi;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.content.api.metadata.ContentMetadataApi;
import yeobaek.backend.content.api.metadata.ContentMetadataDetailResponse;
import yeobaek.backend.content.api.metadata.ContentMetadataResponse;
import yeobaek.backend.shared.identity.ContentId;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentMetadataQueryService {

    private final ContentApi contents;
    private final ContentMetadataApi metadata;

    public List<Summary> search(ContentKind kind, String keyword) {
        return metadata.search(kind, keyword).stream().map(this::summary).toList();
    }

    public Detail getDetail(ContentId contentId) {
        contents.requireAvailable(contentId);
        ContentMetadataDetailResponse found = metadata.get(contentId);
        Summary summary = summary(found);
        return new Detail(summary.contentId(), summary.kind(), summary.title(), summary.creators(),
                summary.publisher(), summary.publishedYear(), summary.coverImageUrl(), summary.unitCount(),
                found.sections().stream().map(section -> new Section(section.id(), section.title(),
                        section.sequence(), section.startUnitSequence(), section.endUnitSequence())).toList());
    }

    private Summary summary(ContentMetadataResponse found) {
        var publication = found.publication();
        String publisher = publication == null || publication.publisher() == null
                ? null : publication.publisher().name();
        Integer publishedYear = publication == null ? null : publication.publishedYear();
        return new Summary(found.contentId(), contents.getContent(found.contentId()).kind(), found.title(),
                found.creators().stream().map(creator -> creator.name()).toList(), publisher, publishedYear,
                found.coverImageUrl(), found.unitCount());
    }

    public record Summary(ContentId contentId, ContentKind kind, String title, List<String> creators,
                          String publisher, Integer publishedYear, String coverImageUrl, int unitCount) {

        public Summary {
            creators = List.copyOf(creators);
        }
    }

    public record Detail(ContentId contentId, ContentKind kind, String title, List<String> creators,
                         String publisher, Integer publishedYear, String coverImageUrl, int unitCount,
                         List<Section> sections) {

        public Detail {
            creators = List.copyOf(creators);
            sections = List.copyOf(sections);
        }
    }

    public record Section(long sectionId, String title, int sequence, int startUnitSequence, int endUnitSequence) {
    }
}
