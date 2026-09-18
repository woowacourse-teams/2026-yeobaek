package yeobaek.backend.book.controller;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.boot.jackson.JacksonMixin;
import yeobaek.backend.book.domain.vo.BookTitle;

@JacksonMixin(BookTitle.class)
public abstract class BookTitleMixin {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public BookTitleMixin(String value) {
    }

    @JsonValue
    public abstract String value();
}
