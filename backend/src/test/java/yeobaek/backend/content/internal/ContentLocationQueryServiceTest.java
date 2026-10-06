package yeobaek.backend.content.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.content.api.location.ContentLocationNotFoundException;
import yeobaek.backend.content.api.location.LocationKind;
import yeobaek.backend.content.persistence.ContentJpaEntity;
import yeobaek.backend.content.persistence.ContentLocationJpaEntity;
import yeobaek.backend.content.persistence.ContentLocationRepository;
import yeobaek.backend.content.internal.location.ContentLocationProvider;
import yeobaek.backend.shared.identity.ContentLocationId;

class ContentLocationQueryServiceTest {

    @Test
    void returnsSentenceKindWithItsParentPassageContext() {
        ContentLocationRepository repository = mock(ContentLocationRepository.class);
        ContentLocationProvider provider = mock(ContentLocationProvider.class);
        ContentLocationJpaEntity entity = mock(ContentLocationJpaEntity.class);
        ContentJpaEntity content = mock(ContentJpaEntity.class);
        ContentLocationId sentence = new ContentLocationId(10L);
        ContentLocationId passage = new ContentLocationId(11L);
        given(repository.findById(10L)).willReturn(Optional.of(entity));
        given(entity.getContent()).willReturn(content);
        given(content.getId()).willReturn(20L);
        given(content.contentKind()).willReturn(ContentKind.BOOK);
        given(entity.locationKind()).willReturn(LocationKind.SENTENCE);
        given(provider.findPassageContext(sentence, LocationKind.SENTENCE))
                .willReturn(Optional.of(new ContentLocationProvider.PassageContext(passage, 3)));

        var result = new ContentLocationQueryService(repository, Map.of(ContentKind.BOOK, provider)).get(sentence);

        assertThat(result.kind()).isEqualTo(LocationKind.SENTENCE);
        assertThat(result.passageLocationId()).isEqualTo(passage);
        assertThat(result.passageSequence()).isEqualTo(3);
        assertThat(result.contentId().value()).isEqualTo(20L);
    }

    @Test
    void rejectsMissingCanonicalLocation() {
        ContentLocationRepository repository = mock(ContentLocationRepository.class);
        var service = new ContentLocationQueryService(repository, Map.of());
        given(repository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(new ContentLocationId(99L)))
                .isInstanceOf(ContentLocationNotFoundException.class);
    }

    @Test
    void returnsPassageAsItsOwnPassageContext() {
        ContentLocationRepository repository = mock(ContentLocationRepository.class);
        ContentLocationProvider provider = mock(ContentLocationProvider.class);
        ContentLocationJpaEntity entity = mock(ContentLocationJpaEntity.class);
        ContentJpaEntity content = mock(ContentJpaEntity.class);
        ContentLocationId passage = new ContentLocationId(30L);
        given(repository.findById(30L)).willReturn(Optional.of(entity));
        given(entity.getContent()).willReturn(content);
        given(content.getId()).willReturn(40L);
        given(content.contentKind()).willReturn(ContentKind.BOOK);
        given(entity.locationKind()).willReturn(LocationKind.PASSAGE);
        given(provider.findPassageContext(passage, LocationKind.PASSAGE))
                .willReturn(Optional.of(new ContentLocationProvider.PassageContext(passage, 7)));

        var result = new ContentLocationQueryService(repository, Map.of(ContentKind.BOOK, provider)).get(passage);

        assertThat(result.kind()).isEqualTo(LocationKind.PASSAGE);
        assertThat(result.passageLocationId()).isEqualTo(passage);
        assertThat(result.passageSequence()).isEqualTo(7);
    }

    @Test
    void propagatesAnUnsupportedContentProviderFailure() {
        ContentLocationRepository repository = mock(ContentLocationRepository.class);
        ContentLocationJpaEntity entity = mock(ContentLocationJpaEntity.class);
        ContentJpaEntity content = mock(ContentJpaEntity.class);
        ContentKind future = new ContentKind("FUTURE_CONTENT");
        given(repository.findById(50L)).willReturn(Optional.of(entity));
        given(entity.getContent()).willReturn(content);
        given(content.contentKind()).willReturn(future);

        assertThatThrownBy(() -> new ContentLocationQueryService(repository, Map.of())
                .get(new ContentLocationId(50L)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
