package yeobaek.backend.web.v2.reading;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import yeobaek.backend.application.content.ContentReadingQueryService.BodyResult;
import yeobaek.backend.application.content.ContentReadingQueryService.PassageResult;
import yeobaek.backend.application.content.ContentReadingQueryService.SentenceResult;
import yeobaek.backend.application.reading.ReadingActivityResult;
import yeobaek.backend.application.reading.ReadingProgressCommandService.ProgressResult;
import yeobaek.backend.content.api.location.LocationKind;
import yeobaek.backend.space.api.SpaceKind;
import yeobaek.backend.web.v2.common.ContentCardResponse;

public final class ReadingResponses {

    private ReadingResponses() {
    }

    public record Body(List<Passage> passages) {

        public Body {
            passages = List.copyOf(passages);
        }
    }

    public record Passage(@Schema(description = "canonical 문단 위치 ID") Long locationId,
                          @Schema(example = "PASSAGE") LocationKind locationKind, int sequence, long chapterId,
                          List<Sentence> sentences) {

        public Passage {
            sentences = List.copyOf(sentences);
        }
    }

    public record Sentence(@Schema(description = "canonical 문장 위치 ID") Long locationId,
                           @Schema(example = "SENTENCE") LocationKind locationKind, int sequence, String content,
                           long commentCount) {
    }

    public record Progress(int lastReadPassageSequence, int progressRate, LocalDateTime lastReadAt) {
    }

    public record Reading(ReadingSpace space, ContentCardResponse content, int lastReadPassageSequence,
                          int progressRate, LocalDateTime lastReadAt) {
    }

    public record ReadingSpace(@Schema(description = "canonical 공간 ID") Long spaceId,
                               @Schema(description = "공간 종류") SpaceKind kind,
                               @Schema(description = "종류별 독서 공간 정보") SpaceData data) {
    }

    @Schema(oneOf = {ClubSpace.class, PublicRoomSpace.class})
    public interface SpaceData {
    }

    public record ClubSpace(String name) implements SpaceData {
    }

    public record PublicRoomSpace() implements SpaceData {
    }

    public static Body from(BodyResult result) {
        return new Body(result.passages().stream().map(ReadingResponses::from).toList());
    }

    private static Passage from(PassageResult result) {
        return new Passage(result.locationId().value(), LocationKind.PASSAGE, result.sequence(), result.sectionId(),
                result.sentences().stream().map(ReadingResponses::from).toList());
    }

    private static Sentence from(SentenceResult result) {
        return new Sentence(result.locationId().value(), LocationKind.SENTENCE, result.sequence(), result.content(),
                result.commentCount());
    }

    public static Progress from(ProgressResult result) {
        return new Progress(result.lastReadPassageSequence(), result.progressRate(), result.lastReadAt());
    }

    public static Reading from(ReadingActivityResult result, SpaceData spaceData, ContentCardResponse content) {
        return new Reading(new ReadingSpace(result.space().spaceId().value(), result.space().kind(),
                spaceData), content,
                result.lastReadPassageSequence(), result.progressRate(), result.lastReadAt());
    }
}
