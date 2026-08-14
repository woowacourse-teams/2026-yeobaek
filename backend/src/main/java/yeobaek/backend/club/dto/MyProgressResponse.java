package yeobaek.backend.club.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record MyProgressResponse(
        @Schema(description = "최근 열람 본문의 전체 순서") int lastReadPassageSequence,
        @Schema(description = "진도율 (0~100 정수, 반올림)") int progressRate,
        @Schema(description = "마지막으로 읽은 시간") LocalDateTime lastReadAt
) {
}
