package yeobaek.backend.book.controller;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.boot.jackson.JacksonMixin;
import yeobaek.backend.book.domain.vo.AuthorName;

@JacksonMixin(AuthorName.class)
public abstract class AuthorNameMixin {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public AuthorNameMixin(String value) {
    }

    @JsonValue
    public abstract String value();
}
