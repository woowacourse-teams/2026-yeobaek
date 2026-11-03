package yeobaek.backend.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record BookGenresUpdateRequest(
        @JsonProperty(required = true)
        @Schema(description = "장르 목록. null/빈 배열은 제거한다.", types = {"array", "null"},
                requiredMode = Schema.RequiredMode.REQUIRED) List<String> genres
) {
    public BookGenresUpdateRequest {
        if (genres != null) {
            genres = Collections.unmodifiableList(new ArrayList<>(genres));
        }
    }

    @Override
    public List<String> genres() {
        return genres == null ? null : Collections.unmodifiableList(genres);
    }
}
