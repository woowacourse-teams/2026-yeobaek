package yeobaek.backend.web.v2.content;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import yeobaek.backend.application.content.ContentMetadataQueryService.Detail;
import yeobaek.backend.application.content.ContentMetadataQueryService.Section;
import yeobaek.backend.application.content.ContentMetadataQueryService.Summary;
import yeobaek.backend.content.api.ContentKind;

public final class ContentResponses {

    private ContentResponses() {
    }

    @Schema(description = "컨텐츠 목록")
    public record Contents(@Schema(description = "조회된 컨텐츠") List<SummaryResponse> contents) {

        public Contents {
            contents = List.copyOf(contents);
        }
    }

    public record SummaryResponse(@Schema(description = "canonical 컨텐츠 ID") Long contentId,
                                  @Schema(description = "컨텐츠 종류", example = "BOOK") ContentKind kind,
                                  @Schema(description = "종류별 요약 정보. BOOK은 BookSummaryData") SummaryData data) {
    }

    @Schema(oneOf = BookSummaryData.class)
    public interface SummaryData {
    }

    public record BookSummaryData(String title, List<String> authors, String publisher, Integer publishedYear,
                                  String coverImageUrl, int passageCount) implements SummaryData {

        public BookSummaryData {
            authors = List.copyOf(authors);
        }
    }

    public record DetailResponse(@Schema(description = "canonical 컨텐츠 ID") Long contentId,
                                 @Schema(description = "컨텐츠 종류", example = "BOOK") ContentKind kind,
                                 @Schema(description = "종류별 상세 정보. BOOK은 BookDetailData") DetailData data) {
    }

    @Schema(oneOf = BookDetailData.class)
    public interface DetailData {
    }

    public record BookDetailData(String title, List<String> authors, String publisher, Integer publishedYear,
                                 String coverImageUrl, int passageCount, List<Chapter> chapters)
            implements DetailData {

        public BookDetailData {
            authors = List.copyOf(authors);
            chapters = List.copyOf(chapters);
        }
    }

    public record Chapter(long chapterId, String title, int sequence, int startPassageSequence,
                          int endPassageSequence) {
    }

    static SummaryResponse book(Summary result) {
        return new SummaryResponse(result.contentId().value(), result.kind(), new BookSummaryData(result.title(),
                result.creators(), result.publisher(), result.publishedYear(), result.coverImageUrl(),
                result.unitCount()));
    }

    static DetailResponse book(Detail result) {
        return new DetailResponse(result.contentId().value(), result.kind(), new BookDetailData(result.title(),
                result.creators(), result.publisher(), result.publishedYear(), result.coverImageUrl(),
                result.unitCount(), result.sections().stream().map(ContentResponses::from).toList()));
    }

    private static Chapter from(Section section) {
        return new Chapter(section.sectionId(), section.title(), section.sequence(), section.startUnitSequence(),
                section.endUnitSequence());
    }
}
