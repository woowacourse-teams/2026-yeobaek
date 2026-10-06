package yeobaek.backend.web.book.controller;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.boot.jackson.JacksonMixin;
import yeobaek.backend.content.api.value.BookTitle;

@JacksonMixin(BookTitle.class)
public abstract class BookTitleMixin {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public BookTitleMixin(String value) {
    }

    @JsonValue
    public abstract String value();
}
