package yeobaek.backend.web.v2.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.boot.jackson.JacksonMixin;
import yeobaek.backend.appreciation.api.AppreciationKind;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.content.api.location.LocationKind;
import yeobaek.backend.space.api.SpaceKind;

public interface V2KindJsonConfiguration {

    @JacksonMixin(ContentKind.class)
    public abstract static class ContentKindMixin {

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        protected ContentKindMixin(String value) {
        }

        @JsonValue
        public abstract String value();
    }

    @JacksonMixin(SpaceKind.class)
    public abstract static class SpaceKindMixin {

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        protected SpaceKindMixin(String value) {
        }

        @JsonValue
        public abstract String value();
    }

    @JacksonMixin(AppreciationKind.class)
    public abstract static class AppreciationKindMixin {

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        protected AppreciationKindMixin(String value) {
        }

        @JsonValue
        public abstract String value();
    }

    @JacksonMixin(LocationKind.class)
    public abstract static class LocationKindMixin {

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        protected LocationKindMixin(String value) {
        }

        @JsonValue
        public abstract String value();
    }
}
