package yeobaek.backend.web.comment.controller;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.boot.jackson.JacksonMixin;
import yeobaek.backend.appreciation.api.comment.CommentContent;

@JacksonMixin(CommentContent.class)
public abstract class CommentContentMixin {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public CommentContentMixin(String value) {
    }

    @JsonValue
    public abstract String value();
}
