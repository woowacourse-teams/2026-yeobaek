package yeobaek.backend.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import yeobaek.backend.content.api.Content;
import yeobaek.backend.content.api.ContentApi;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.content.internal.ContentBodyService;
import yeobaek.backend.content.internal.body.ContentBodyProvider;
import yeobaek.backend.shared.identity.ContentId;

class ContentCapabilityWiringTest {

    @Test
    void injectsTheTypedProviderMapIntoTheService() {
        ContentId contentId = new ContentId(7L);
        ContentApi contents = mock(ContentApi.class);
        ContentBodyProvider bodies = mock(ContentBodyProvider.class);
        Content content = mock(Content.class);
        given(content.kind()).willReturn(ContentKind.BOOK);
        given(contents.getContent(contentId)).willReturn(content);
        given(bodies.supportedKind()).willReturn(ContentKind.BOOK);
        given(bodies.findPassages(contentId, 1, 3)).willReturn(List.of());

        try (var context = new AnnotationConfigApplicationContext()) {
            context.register(ContentCapabilityConfiguration.class);
            context.registerBean(ContentApi.class, () -> contents);
            context.registerBean(ContentBodyProvider.class, () -> bodies);
            context.registerBean(ContentBodyService.class);
            context.refresh();

            assertThat(context.getBean(ContentBodyService.class).findPassages(contentId, 1, 3)).isEmpty();
        }
    }
}
