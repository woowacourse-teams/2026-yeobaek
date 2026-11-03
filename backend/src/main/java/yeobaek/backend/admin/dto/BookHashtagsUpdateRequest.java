package yeobaek.backend.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record BookHashtagsUpdateRequest(
        @JsonProperty(required = true)
        @Schema(description = "이 책의 해시태그 목록을 교체한다(최대 5개, 각 10자). null 또는 []를 보내면 등록된 해시태그를 모두 삭제한다.",
                types = {"array", "null"}, requiredMode = Schema.RequiredMode.REQUIRED)
        List<String> hashtags
) {
}
