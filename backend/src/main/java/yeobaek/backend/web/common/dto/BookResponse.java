package yeobaek.backend.web.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "ClubBookResponse")
public record BookResponse(
        @Schema(description = "도서 ID") Long bookId,
        @Schema(description = "도서 제목") String title,
        @Schema(description = "작가 이름 목록") List<String> authors,
        @Schema(description = "표지 이미지 공개 URL", nullable = true) String coverImageUrl,
        @Schema(description = "본문 개수") int passageCount,
        @Schema(description = "도서 상태", allowableValues = {"ACTIVE", "DELETED"}) Status status
) {

    public BookResponse {
        authors = List.copyOf(authors);
    }

    public enum Status {
        ACTIVE,
        DELETED
    }
}
