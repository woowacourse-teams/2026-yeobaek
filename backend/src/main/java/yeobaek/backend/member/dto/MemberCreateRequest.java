package yeobaek.backend.member.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import yeobaek.backend.member.domain.vo.Nickname;

public record MemberCreateRequest(
        @JsonProperty(required = true)
        @JsonSetter(nulls = Nulls.FAIL)
        @Schema(type = "string", description = "닉네임 (1~20자, 공백만은 불가, 중복 불가)") @NotNull Nickname nickname
) {
}
