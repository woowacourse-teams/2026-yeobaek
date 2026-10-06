package yeobaek.backend.web.v2.space;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.jackson.JacksonMixin;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.space.api.SpaceKind;
import yeobaek.backend.space.api.club.ClubName;
import yeobaek.backend.space.api.club.JoinCode;

public interface SpaceRequests {

    public record Create(@NotNull @Schema(description = "공간 종류", example = "CLUB") SpaceKind kind,
                         @NotNull @Valid @Schema(description = "종류별 생성 입력. CLUB은 CreateClubData")
                         CreateData data) {
    }

    @Schema(oneOf = CreateClubData.class)
    public interface CreateData {
    }

    public record CreateClubData(@NotNull @Schema(description = "1~20자 모임 이름") ClubName name,
                                 @NotNull @Schema(description = "canonical 컨텐츠 ID",
                                         implementation = Long.class) ContentId contentId)
            implements CreateData {
    }

    public record Join(@NotNull @Schema(description = "공간 종류", example = "CLUB") SpaceKind kind,
                       @NotNull @Valid @Schema(description = "종류별 가입 입력. CLUB은 JoinClubData") JoinData data) {
    }

    @Schema(oneOf = JoinClubData.class)
    public interface JoinData {
    }

    public record JoinClubData(@NotNull @Schema(description = "6자 대문자·숫자 참여 코드") JoinCode joinCode)
            implements JoinData {
    }

    @JacksonMixin(Create.class)
    public abstract static class CreateMixin {

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "kind")
        public abstract CreateData data();
    }

    @JacksonMixin(Join.class)
    public abstract static class JoinMixin {

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "kind")
        public abstract JoinData data();
    }
}
