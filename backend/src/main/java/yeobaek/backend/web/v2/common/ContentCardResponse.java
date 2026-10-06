package yeobaek.backend.web.v2.common;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import yeobaek.backend.content.api.ContentKind;

@Schema(description = "공간과 독서 기록에서 공통으로 사용하는 컨텐츠 요약")
public record ContentCardResponse(@Schema(description = "canonical 컨텐츠 ID") Long contentId,
                                  @Schema(description = "컨텐츠 종류", example = "BOOK") ContentKind kind,
                                  @Schema(description = "종류별 카드 정보. BOOK은 BookData") Data data) {

    @Schema(oneOf = BookData.class)
    public interface Data {
    }

    public record BookData(String title, List<String> authors, String coverImageUrl, int passageCount, Status status)
            implements Data {

        public BookData {
            authors = List.copyOf(authors);
        }
    }

    public enum Status {
        ACTIVE,
        DELETED
    }
}
