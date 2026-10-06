package yeobaek.backend.web.club.controller;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.boot.jackson.JacksonMixin;
import yeobaek.backend.space.api.club.JoinCode;

@JacksonMixin(JoinCode.class)
public abstract class JoinCodeMixin {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public JoinCodeMixin(String value) {
    }

    @JsonValue
    public abstract String value();
}
