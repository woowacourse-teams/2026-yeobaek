package yeobaek.backend.web.v2.space;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import yeobaek.backend.application.space.query.SpaceQueryResult;
import yeobaek.backend.application.space.query.SpaceQueryService;
import yeobaek.backend.application.space.query.UnsupportedSpaceQueryException;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.space.api.SpaceKind;
import yeobaek.backend.web.v2.content.ContentWebAdapterRegistry;
import yeobaek.backend.web.v2.reading.ReadingResponses;

@Component
@RequiredArgsConstructor
public class PublicRoomSpaceWebAdapter implements SpaceWebAdapter {

    private final SpaceQueryService spaces;
    private final ContentWebAdapterRegistry contentAdapters;

    @Override
    public SpaceKind kind() {
        return SpaceKind.PUBLIC_ROOM;
    }

    @Override
    public SpaceResponses.Space create(MemberId actorId, SpaceRequests.CreateData data) {
        throw unsupported("공개방은 컨텐츠 적재 시 생성됩니다.");
    }

    @Override
    public SpaceResponses.Space join(MemberId actorId, SpaceRequests.JoinData data) {
        throw unsupported("공개방은 참여 코드 가입을 지원하지 않습니다.");
    }

    @Override
    public List<SpaceQueryResult> findPublic(MemberId actorId, String sort) {
        PublicRoomSort.from(sort);
        return spaces.findPublic(actorId, kind());
    }

    @Override
    public SpaceResponses.Space map(SpaceQueryResult result) {
        SpaceQueryResult.Data data = result.data();
        if (data instanceof SpaceQueryResult.PublicRoomSummary summary) {
            return new SpaceResponses.Space(result.spaceId().value(), result.kind(),
                    new SpaceResponses.PublicRoomSummary(contentAdapters.card(summary.content()),
                            SpaceResponses.progress(summary.myProgress())));
        }
        if (data instanceof SpaceQueryResult.PublicRoomVisitedSummary visited) {
            return new SpaceResponses.Space(result.spaceId().value(), result.kind(),
                    new SpaceResponses.PublicRoomVisited(contentAdapters.card(visited.content()),
                            SpaceResponses.progress(visited.myProgress()), visited.lastVisitedAt()));
        }
        if (data instanceof SpaceQueryResult.PublicRoomDetail detail) {
            return new SpaceResponses.Space(result.spaceId().value(), result.kind(),
                    new SpaceResponses.PublicRoomDetail(contentAdapters.card(detail.content()),
                            SpaceResponses.progress(detail.myProgress()), detail.lastVisitedAt()));
        }
        throw new IllegalStateException("지원하지 않는 공개방 조회 결과입니다: " + data.getClass().getName());
    }

    @Override
    public ReadingResponses.SpaceData mapReading(yeobaek.backend.application.reading.ReadingActivityResult
                                                          .ReadingSpace space) {
        if (!(space.data() instanceof yeobaek.backend.application.reading.ReadingActivityResult.ReadingSpace
                .PublicRoom)) {
            throw new IllegalArgumentException("공간 종류와 최근 독서 결과가 일치하지 않습니다.");
        }
        return new ReadingResponses.PublicRoomSpace();
    }

    private static UnsupportedSpaceQueryException unsupported(String message) {
        return new UnsupportedSpaceQueryException(SpaceKind.PUBLIC_ROOM, message);
    }

    private enum PublicRoomSort {
        MOST_VISITED;

        private static PublicRoomSort from(String value) {
            if (value == null) {
                return MOST_VISITED;
            }
            if (value.isBlank()) {
                throw new IllegalArgumentException("공개방 정렬 값은 비어 있을 수 없습니다.");
            }
            try {
                return valueOf(value);
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("지원하지 않는 공개방 정렬입니다: " + value, exception);
            }
        }
    }
}
