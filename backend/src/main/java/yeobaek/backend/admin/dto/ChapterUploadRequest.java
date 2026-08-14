package yeobaek.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record ChapterUploadRequest(
        @Schema(description = "목차 제목 (1~100자)") String title,
        @Schema(description = "본문 목록 (등장 순서로 전체 순서 부여)") List<PassageUploadRequest> passages
) {

    public ChapterUploadRequest {
        passages = passages == null ? List.of() : List.copyOf(passages);
    }
}
