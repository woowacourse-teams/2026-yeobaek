package yeobaek.backend.web.v2.space;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import yeobaek.backend.application.club.ClubCommandResult;
import yeobaek.backend.application.space.query.SpaceQueryResult;
import yeobaek.backend.space.api.SpaceKind;
import yeobaek.backend.web.v2.common.ContentCardResponse;

public final class SpaceResponses {

    private SpaceResponses() {
    }

    public record Spaces(@Schema(description = "공간 목록") List<Space> spaces) {

        public Spaces {
            spaces = List.copyOf(spaces);
        }
    }

    public record Space(@Schema(description = "canonical 공간 ID") Long spaceId,
                        @Schema(description = "공간 종류", example = "CLUB") SpaceKind kind,
                        @Schema(description = "종류·조회 기능별 공간 정보") Data data) {
    }

    @Schema(oneOf = {CreatedClub.class, JoinedClub.class, ClubSummary.class, ClubDetail.class,
            PublicRoomSummary.class, PublicRoomVisited.class, PublicRoomDetail.class})
    public interface Data {
    }

    public record CreatedClub(String name, String joinCode, ContentCardResponse content) implements Data {
    }

    public record JoinedClub(String name, ContentCardResponse content) implements Data {
    }

    public record ClubSummary(String name, long memberCount, ContentCardResponse content, Progress myProgress)
            implements Data {
    }

    public record ClubDetail(String name, String joinCode, ContentCardResponse content, Progress myProgress,
                             List<Member> members) implements Data {

        public ClubDetail {
            members = List.copyOf(members);
        }
    }

    public record PublicRoomSummary(ContentCardResponse content, Progress myProgress) implements Data {
    }

    public record PublicRoomVisited(ContentCardResponse content, Progress myProgress, LocalDateTime lastVisitedAt)
            implements Data {
    }

    public record PublicRoomDetail(ContentCardResponse content, Progress myProgress, LocalDateTime lastVisitedAt)
            implements Data {
    }

    public record Progress(@Schema(description = "마지막으로 읽은 문단 순서") int lastReadPassageSequence,
                           @Schema(description = "반올림한 진행률(%)") int progressRate,
                           @Schema(description = "마지막 독서 시각") LocalDateTime lastReadAt) {
    }

    public record Member(Long memberId, String nickname, boolean mine, boolean blocked) {
    }

    public static Space created(ClubCommandResult result, ContentCardResponse content) {
        return new Space(result.club().id().value(), result.club().kind(), new CreatedClub(result.club().name(),
                result.club().joinCode(), content));
    }

    public static Space joined(ClubCommandResult result, ContentCardResponse content) {
        return new Space(result.club().id().value(), result.club().kind(), new JoinedClub(result.club().name(),
                content));
    }

    static Progress progress(SpaceQueryResult.Progress progress) {
        if (progress == null) {
            return null;
        }
        return new Progress(progress.lastReadPassageSequence(), progress.progressRate(), progress.lastReadAt());
    }
}
