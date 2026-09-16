package yeobaek.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record PassageUploadRequest(
        @Schema(description = "문장 목록 (최소 1개, 등장 순서로 문단 내 순서 부여)")
        @Valid @NotNull List<@NotNull SentenceUploadRequest> sentences
) {

    public PassageUploadRequest {
        if (sentences != null) {
            sentences = Collections.unmodifiableList(new ArrayList<>(sentences));
        }
    }

    @Override
    public List<SentenceUploadRequest> sentences() {
        return sentences == null ? null : Collections.unmodifiableList(sentences);
    }
}
