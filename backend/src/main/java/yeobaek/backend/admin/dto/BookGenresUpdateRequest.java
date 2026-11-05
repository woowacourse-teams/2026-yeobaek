package yeobaek.backend.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import tools.jackson.databind.annotation.JsonDeserialize;
import yeobaek.backend.admin.controller.StrictStringDeserializer;

public record BookGenresUpdateRequest(
        @JsonProperty(required = true)
        @JsonDeserialize(contentUsing = StrictStringDeserializer.class)
        @Schema(description = "이 책의 장르 목록을 교체한다. 장르당 최대 255자(Unicode 코드 포인트 기준). null 또는 []를 보내면 등록된 장르를 모두 삭제한다.", types = {"array", "null"},
                requiredMode = Schema.RequiredMode.REQUIRED) List<String> genres
) {
}
