package yeobaek.backend.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record BookHashtagsUpdateRequest(
        @JsonProperty(required = true)
        @Schema(description = "최종 해시태그 목록 (최대 5개, 각 10자). null/빈 배열은 제거한다.",
                types = {"array", "null"}, requiredMode = Schema.RequiredMode.REQUIRED)
        List<String> hashtags
) {
    public BookHashtagsUpdateRequest {
        if (hashtags != null) {
            hashtags = Collections.unmodifiableList(new ArrayList<>(hashtags));
        }
    }

    @Override
    public List<String> hashtags() {
        return hashtags == null ? null : Collections.unmodifiableList(hashtags);
    }
}
