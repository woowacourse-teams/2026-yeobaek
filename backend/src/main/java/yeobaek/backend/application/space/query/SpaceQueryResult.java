package yeobaek.backend.application.space.query;

import java.time.LocalDateTime;
import java.util.List;
import yeobaek.backend.application.content.ContentCardResult;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.space.api.SpaceKind;

public record SpaceQueryResult(SpaceId spaceId, SpaceKind kind, Data data) {

    public interface Data {
    }

    public record Progress(int lastReadPassageSequence, int progressRate, LocalDateTime lastReadAt) {
    }

    public record Member(MemberId memberId, String nickname, boolean mine, boolean blocked) {
    }

    public record ClubSummary(String name, long memberCount, ContentCardResult content, Progress myProgress)
            implements Data {
    }

    public record ClubDetail(String name, String joinCode, ContentCardResult content, Progress myProgress,
                             List<Member> members) implements Data {

        public ClubDetail {
            members = List.copyOf(members);
        }
    }

    public record PublicRoomSummary(ContentCardResult content, Progress myProgress)
            implements Data {
    }

    public record PublicRoomVisitedSummary(ContentCardResult content, Progress myProgress,
                                           LocalDateTime lastVisitedAt)
            implements Data {
    }

    public record PublicRoomDetail(ContentCardResult content, Progress myProgress, LocalDateTime lastVisitedAt)
            implements Data {
    }
}
