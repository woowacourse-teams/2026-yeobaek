package yeobaek.backend.member.controller;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.boot.jackson.JacksonMixin;
import yeobaek.backend.member.domain.vo.Nickname;

@JacksonMixin(Nickname.class)
public abstract class NicknameMixin {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public NicknameMixin(String value) {
    }

    @JsonValue
    public abstract String value();
}
