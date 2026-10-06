package yeobaek.backend.web.book.controller;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.boot.jackson.JacksonMixin;
import yeobaek.backend.content.api.value.ChapterTitle;

@JacksonMixin(ChapterTitle.class)
public abstract class ChapterTitleMixin {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public ChapterTitleMixin(String value) {
    }

    @JsonValue
    public abstract String value();
}
