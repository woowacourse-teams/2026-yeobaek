package yeobaek.backend.book.controller;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.boot.jackson.JacksonMixin;
import yeobaek.backend.book.domain.vo.BookDescription;

@JacksonMixin(BookDescription.class)
public abstract class BookDescriptionMixin {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public BookDescriptionMixin(String value) {
    }

    @JsonValue
    public abstract String value();
}
