package yeobaek.backend.book.controller;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.boot.jackson.JacksonMixin;
import yeobaek.backend.book.domain.vo.ChapterTitle;

@JacksonMixin(ChapterTitle.class)
public abstract class ChapterTitleMixin {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public ChapterTitleMixin(String value) {
    }

    @JsonValue
    public abstract String value();
}
