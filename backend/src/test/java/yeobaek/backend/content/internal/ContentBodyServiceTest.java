package yeobaek.backend.content.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import yeobaek.backend.content.api.ContentApi;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.content.api.body.ContentBodyApi;
import yeobaek.backend.content.api.Content;
import yeobaek.backend.content.internal.body.ContentBodyProvider;
import yeobaek.backend.shared.identity.ContentId;

class ContentBodyServiceTest {

    @Test
    void dispatchesAThirdContentKindWithoutChangingTheCoreService() {
        ContentId contentId = new ContentId(91L);
        ContentApi contents = mock(ContentApi.class);
        ContentBodyProvider provider = mock(ContentBodyProvider.class);
        Content content = new ThirdContent(contentId);
        given(contents.getContent(contentId)).willReturn(content);
        given(provider.findPassages(contentId, 1, 3)).willReturn(List.of());
        var service = new ContentBodyService(contents, Map.of(ThirdContent.KIND, provider));

        List<ContentBodyApi.Passage> passages = service.findPassages(contentId, 1, 3);

        assertThat(passages).isEmpty();
    }

    private record ThirdContent(ContentId id) implements Content {

        private static final ContentKind KIND = new ContentKind("THIRD_CONTENT");

        @Override
        public ContentKind kind() {
            return KIND;
        }

        @Override
        public boolean available() {
            return true;
        }
    }
}
