package yeobaek.backend.club.controller;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.boot.jackson.JacksonMixin;
import yeobaek.backend.club.domain.vo.ClubName;

@JacksonMixin(ClubName.class)
public abstract class ClubNameMixin {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public ClubNameMixin(String value) {
    }

    @JsonValue
    public abstract String value();
}
