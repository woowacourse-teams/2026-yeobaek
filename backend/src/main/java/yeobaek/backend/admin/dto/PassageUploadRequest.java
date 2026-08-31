package yeobaek.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record PassageUploadRequest(
        @Schema(description = "문장 목록 (최소 1개, 등장 순서로 문단 내 순서 부여)") List<SentenceUploadRequest> sentences
) {

    public PassageUploadRequest {
        sentences = sentences == null ? List.of() : List.copyOf(sentences);
    }
}
