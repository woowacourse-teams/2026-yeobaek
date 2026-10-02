package yeobaek.backend.content.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;
import org.junit.jupiter.api.Test;
import yeobaek.backend.content.api.ContentApi;
import yeobaek.backend.content.api.ContentBodyApi;
import yeobaek.backend.content.api.ContentBodyProvider;
import yeobaek.backend.content.api.ContentBodyProviderRegistry;
import yeobaek.backend.content.domain.Content;
import yeobaek.backend.foundation.identity.ContentId;

class ContentBodyServiceTest {

    @Test
    void dispatchesAThirdContentKindWithoutChangingTheCoreService() {
        ContentId contentId = new ContentId(91L);
        ContentApi contents = mock(ContentApi.class);
        ContentBodyProvider provider = mock(ContentBodyProvider.class);
        ContentBodyProviderRegistry providers = mock(ContentBodyProviderRegistry.class);
        Content content = new ThirdContent(contentId);
        given(contents.getContent(contentId)).willReturn(content);
        given(providers.get(ThirdContent.KIND)).willReturn(provider);
        given(provider.findPassages(contentId, 1, 3)).willReturn(List.of());
        var service = new ContentBodyService(contents, providers);

        List<ContentBodyApi.Passage> passages = service.findPassages(contentId, 1, 3);

        assertThat(passages).isEmpty();
    }

    private record ThirdContent(ContentId id) implements Content {

        private static final String KIND = "THIRD_CONTENT";

        @Override
        public String kind() {
            return KIND;
        }

        @Override
        public boolean available() {
            return true;
        }
    }
}
