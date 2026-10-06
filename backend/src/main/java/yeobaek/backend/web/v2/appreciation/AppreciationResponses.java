package yeobaek.backend.web.v2.appreciation;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import yeobaek.backend.application.appreciation.CommentQueryWorkflow.CommentResult;
import yeobaek.backend.application.appreciation.CommentQueryWorkflow.DiscoveryResult;
import yeobaek.backend.application.appreciation.CommentSharingWorkflow.SharedComment;
import yeobaek.backend.appreciation.api.AppreciationKind;

public final class AppreciationResponses {

    private AppreciationResponses() {
    }

    public record Appreciations(List<Appreciation> appreciations) {

        public Appreciations {
            appreciations = List.copyOf(appreciations);
        }
    }

    public record Appreciation(@Schema(description = "canonical 감상 ID") Long appreciationId,
                               @Schema(description = "감상 종류", example = "COMMENT") AppreciationKind kind,
                               Long memberId, String nickname,
                               LocalDateTime createdAt, LocalDateTime updatedAt, boolean mine, Data data) {
    }

    @Schema(oneOf = CommentData.class)
    public interface Data {
    }

    public record CommentData(String content) implements Data {
    }

    public record UnreadCount(long newCommentCount) {
    }

    public record Discovery(List<CommentedLocation> commentedLocations) {

        public Discovery {
            commentedLocations = List.copyOf(commentedLocations);
        }
    }

    public record CommentedLocation(@Schema(description = "canonical 문장 위치 ID") Long locationId,
                                    String content,
                                    @Schema(description = "canonical 문단 위치 ID") Long passageLocationId,
                                    int passageSequence,
                                    int sentenceSequence, boolean future, long commentCount, long unreadCommentCount,
                                    ContentVisibility contentVisibility, LocalDateTime latestCommentCreatedAt) {
    }

    public enum ContentVisibility {
        VISIBLE,
        REVEAL_REQUIRED
    }

    public static Appreciation from(SharedComment result, long actorId) {
        var comment = result.comment();
        return new Appreciation(comment.id().value(), comment.kind(), comment.authorId().value(),
                result.author().nickname(), comment.createdAt(), comment.updatedAt(),
                comment.authorId().value() == actorId, new CommentData(comment.content()));
    }

    public static Appreciation from(CommentResult result, long actorId) {
        var comment = result.comment();
        return new Appreciation(comment.id().value(), comment.kind(), comment.authorId().value(),
                result.author().nickname(), comment.createdAt(), comment.updatedAt(),
                comment.authorId().value() == actorId, new CommentData(comment.content()));
    }

    public static CommentedLocation from(DiscoveryResult result) {
        return new CommentedLocation(result.locationId().value(), result.content(),
                result.passageLocationId().value(), result.passageSequence(), result.sentenceSequence(),
                result.future(), result.commentCount(), result.unreadCommentCount(),
                result.revealRequired() ? ContentVisibility.REVEAL_REQUIRED : ContentVisibility.VISIBLE,
                result.latestCommentCreatedAt());
    }
}
