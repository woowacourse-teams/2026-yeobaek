package yeobaek.backend.book.controller;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.boot.jackson.JacksonMixin;
import yeobaek.backend.book.domain.vo.Publisher;

@JacksonMixin(Publisher.class)
public abstract class PublisherMixin {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public PublisherMixin(String value) {
    }

    @JsonValue
    public abstract String value();
}
