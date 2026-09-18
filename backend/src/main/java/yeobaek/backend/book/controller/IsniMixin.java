package yeobaek.backend.book.controller;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.boot.jackson.JacksonMixin;
import yeobaek.backend.book.domain.vo.Isni;

@JacksonMixin(Isni.class)
public abstract class IsniMixin {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public IsniMixin(String value) {
    }

    @JsonValue
    public abstract String value();
}
