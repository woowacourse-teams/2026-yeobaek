package yeobaek.backend.application.space.query.club;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.application.space.query.SpaceQueryPolicyException;
import yeobaek.backend.application.space.query.SpaceQueryProvider;
import yeobaek.backend.application.space.query.SpaceQueryResult;
import yeobaek.backend.application.space.query.SpaceQueryResult.ClubDetail;
import yeobaek.backend.application.space.query.SpaceQueryResult.ClubSummary;
import yeobaek.backend.application.space.query.SpaceQueryResult.Member;
import yeobaek.backend.application.space.query.SpaceQueryResultFactory;
import yeobaek.backend.collaboration.api.binding.SpaceContentBindingApi;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.member.api.MemberQuery;
import yeobaek.backend.member.api.block.MemberBlockApi;
import yeobaek.backend.reading.api.ReadingProgressApi;
import yeobaek.backend.space.api.SpaceKind;
import yeobaek.backend.space.api.SpaceNotFoundException;
import yeobaek.backend.space.api.club.ClubApi;
import yeobaek.backend.space.api.club.ClubMembershipApi;
import yeobaek.backend.space.api.club.ClubMembershipApi.MembershipResponse;
import yeobaek.backend.space.api.club.ClubResponse;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubSpaceQueryProvider implements SpaceQueryProvider {

    private final ClubApi clubs;
    private final ClubMembershipApi memberships;
    private final SpaceContentBindingApi bindings;
    private final ReadingProgressApi readings;
    private final MemberQuery members;
    private final MemberBlockApi blocks;
    private final SpaceQueryResultFactory results;

    @Override
    public SpaceKind supportedKind() {
        return SpaceKind.CLUB;
    }

    @Override
    public List<SpaceQueryResult> findMine(MemberId actorId) {
        members.getProfile(actorId);
        List<MembershipResponse> joinedMemberships = memberships.findJoinedByMember(actorId);
        var counts = memberships.countJoinedMembers(joinedMemberships.stream()
                .map(membership -> membership.club().clubId()).toList());
        return joinedMemberships.stream().map(membership -> {
            ClubResponse club = membership.club();
            ContentId contentId = requireContent(club.id());
            var content = results.content(contentId);
            var progress = readings.find(actorId, club.id(), contentId).orElse(null);
            return new SpaceQueryResult(club.id(), SpaceKind.CLUB,
                    new ClubSummary(club.name(), counts.getOrDefault(club.clubId(), 0L), content,
                            results.progress(progress, content.unitCount())));
        }).toList();
    }

    @Override
    public SpaceQueryResult findDetail(MemberId actorId, SpaceId spaceId) {
        members.getProfile(actorId);
        ClubResponse club = clubs.findBySpaceId(spaceId).orElseThrow(() -> new SpaceNotFoundException(spaceId,
                "조회할 모임 공간이 존재하지 않습니다: spaceId=" + spaceId.value()));
        List<MembershipResponse> joinedMemberships = memberships.findJoinedByClub(club.clubId());
        boolean joined = joinedMemberships.stream().anyMatch(membership -> membership.memberId().equals(actorId));
        if (!joined) {
            throw new SpaceQueryPolicyException(ErrorCode.SPACE_ACCESS_DENIED,
                    "모임에 참여 중인 회원만 상세 정보를 조회할 수 있습니다: spaceId=" + spaceId.value(),
                    Map.of("actorId", Long.toString(actorId.value()),
                            "spaceId", Long.toString(spaceId.value()),
                            "reason", "SPACE_ACCESS_DENIED"));
        }
        Set<MemberId> memberIds = joinedMemberships.stream().map(MembershipResponse::memberId)
                .collect(Collectors.toSet());
        Set<MemberId> blocked = blocks.findBlockedMemberIds(actorId, memberIds);
        var profiles = members.findProfiles(memberIds);
        ContentId contentId = requireContent(spaceId);
        var content = results.content(contentId);
        var progress = readings.find(actorId, spaceId, contentId).orElse(null);
        List<Member> memberResults = joinedMemberships.stream().map(membership -> {
            MemberId memberId = membership.memberId();
            boolean mine = memberId.equals(actorId);
            return new Member(memberId, profiles.get(memberId).nickname(), mine, !mine && blocked.contains(memberId));
        }).toList();
        return new SpaceQueryResult(spaceId, SpaceKind.CLUB,
                new ClubDetail(club.name(), club.joinCode(), content, results.progress(progress, content.unitCount()),
                        memberResults));
    }

    private ContentId requireContent(SpaceId spaceId) {
        return bindings.findContents(spaceId).stream().findFirst()
                .orElseThrow(() -> new SpaceQueryPolicyException(ErrorCode.CONTENT_NOT_BOUND,
                        "모임에 연결된 컨텐츠가 없습니다: spaceId=" + spaceId.value(),
                        Map.of("spaceId", Long.toString(spaceId.value()), "reason", "CONTENT_NOT_BOUND")));
    }
}
