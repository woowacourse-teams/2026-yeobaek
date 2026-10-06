package yeobaek.backend.web.book.controller;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.boot.jackson.JacksonMixin;
import yeobaek.backend.content.api.value.SentenceContent;

@JacksonMixin(SentenceContent.class)
public abstract class SentenceContentMixin {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public SentenceContentMixin(String value) {
    }

    @JsonValue
    public abstract String value();
}
