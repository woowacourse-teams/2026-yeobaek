package yeobaek.backend.web.v2.space;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import yeobaek.backend.application.club.ClubCreationWorkflow;
import yeobaek.backend.application.club.ClubMembershipWorkflow;
import yeobaek.backend.application.space.query.SpaceQueryResult;
import yeobaek.backend.application.space.query.UnsupportedSpaceQueryException;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.shared.exception.NotFoundException;
import yeobaek.backend.space.api.SpaceKind;
import yeobaek.backend.space.api.club.ClubApi;
import yeobaek.backend.web.v2.content.ContentWebAdapterRegistry;
import yeobaek.backend.web.v2.reading.ReadingResponses;
import yeobaek.backend.web.v2.space.SpaceRequests.CreateClubData;
import yeobaek.backend.web.v2.space.SpaceRequests.JoinClubData;

@Component
@RequiredArgsConstructor
public class ClubSpaceWebAdapter implements SpaceWebAdapter {

    private final ClubCreationWorkflow creations;
    private final ClubMembershipWorkflow memberships;
    private final ClubApi clubs;
    private final ContentWebAdapterRegistry contentAdapters;

    @Override
    public SpaceKind kind() {
        return SpaceKind.CLUB;
    }

    @Override
    public SpaceResponses.Space create(MemberId actorId, SpaceRequests.CreateData data) {
        CreateClubData club = requireData(data, CreateClubData.class);
        var result = creations.create(actorId, club.name(), club.contentId());
        return SpaceResponses.created(result, contentAdapters.card(result.content()));
    }

    @Override
    public SpaceResponses.Space join(MemberId actorId, SpaceRequests.JoinData data) {
        JoinClubData club = requireData(data, JoinClubData.class);
        var found = clubs.findByJoinCode(club.joinCode())
                .orElseThrow(() -> new NotFoundException(ErrorCode.JOIN_CODE_NOT_FOUND,
                        "참여 코드에 해당하는 모임을 찾을 수 없습니다.",
                        Map.of("actorId", Long.toString(actorId.value()), "lookup", "joinCode")));
        var result = memberships.join(actorId, found.id());
        return SpaceResponses.joined(result, contentAdapters.card(result.content()));
    }

    @Override
    public List<SpaceQueryResult> findPublic(MemberId actorId, String sort) {
        throw unsupported();
    }

    @Override
    public SpaceResponses.Space map(SpaceQueryResult result) {
        SpaceQueryResult.Data data = result.data();
        if (data instanceof SpaceQueryResult.ClubSummary summary) {
            return new SpaceResponses.Space(result.spaceId().value(), result.kind(),
                    new SpaceResponses.ClubSummary(summary.name(), summary.memberCount(),
                            contentAdapters.card(summary.content()), SpaceResponses.progress(summary.myProgress())));
        }
        if (data instanceof SpaceQueryResult.ClubDetail detail) {
            return new SpaceResponses.Space(result.spaceId().value(), result.kind(),
                    new SpaceResponses.ClubDetail(detail.name(), detail.joinCode(),
                            contentAdapters.card(detail.content()), SpaceResponses.progress(detail.myProgress()),
                            detail.members().stream().map(member -> new SpaceResponses.Member(
                                    member.memberId().value(), member.nickname(), member.mine(), member.blocked()))
                                    .toList()));
        }
        throw new IllegalStateException("지원하지 않는 모임 조회 결과입니다: " + data.getClass().getName());
    }

    @Override
    public ReadingResponses.SpaceData mapReading(yeobaek.backend.application.reading.ReadingActivityResult
                                                          .ReadingSpace space) {
        var club = requireData(space.data(), yeobaek.backend.application.reading.ReadingActivityResult
                .ReadingSpace.Club.class);
        return new ReadingResponses.ClubSpace(club.name());
    }

    private static <T> T requireData(Object data, Class<T> expected) {
        if (!expected.isInstance(data)) {
            throw new IllegalArgumentException("공간 종류와 요청 data가 일치하지 않습니다.");
        }
        return expected.cast(data);
    }

    private static UnsupportedSpaceQueryException unsupported() {
        return new UnsupportedSpaceQueryException(SpaceKind.CLUB, "모임은 전체 공개 공간 목록을 제공하지 않습니다.");
    }
}
