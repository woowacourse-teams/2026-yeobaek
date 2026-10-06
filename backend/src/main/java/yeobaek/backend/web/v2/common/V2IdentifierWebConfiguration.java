package yeobaek.backend.web.v2.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.boot.jackson.JacksonMixin;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import yeobaek.backend.shared.identity.AppreciationId;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.ContentLocationId;
import yeobaek.backend.shared.identity.SpaceId;

@Configuration(proxyBeanMethods = false)
public class V2IdentifierWebConfiguration implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, SpaceId.class, value -> new SpaceId(Long.valueOf(value)));
        registry.addConverter(String.class, ContentId.class, value -> new ContentId(Long.valueOf(value)));
        registry.addConverter(String.class, ContentLocationId.class,
                value -> new ContentLocationId(Long.valueOf(value)));
        registry.addConverter(String.class, AppreciationId.class,
                value -> new AppreciationId(Long.valueOf(value)));
    }

    @JacksonMixin(ContentId.class)
    public abstract static class ContentIdMixin {

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        protected ContentIdMixin(Long value) {
        }

        @JsonValue
        public abstract Long value();
    }

    @JacksonMixin(ContentLocationId.class)
    public abstract static class ContentLocationIdMixin {

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        protected ContentLocationIdMixin(Long value) {
        }

        @JsonValue
        public abstract Long value();
    }
}
