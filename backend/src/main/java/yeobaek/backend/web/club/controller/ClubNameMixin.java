package yeobaek.backend.web.club.controller;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.boot.jackson.JacksonMixin;
import yeobaek.backend.space.api.club.ClubName;

@JacksonMixin(ClubName.class)
public abstract class ClubNameMixin {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public ClubNameMixin(String value) {
    }

    @JsonValue
    public abstract String value();
}
