package yeobaek.backend.book.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record PassageResponse(
        @Schema(description = "본문 ID") Long passageId,
        @Schema(description = "책 전체 기준 순서") int sequence,
        @Schema(description = "소속 목차 ID") Long chapterId,
        @Schema(description = "문단에 속한 문장 목록") List<SentenceResponse> sentences
) {

    public PassageResponse {
        sentences = List.copyOf(sentences);
    }
}
