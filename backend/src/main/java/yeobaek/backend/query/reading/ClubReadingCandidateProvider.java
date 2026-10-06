package yeobaek.backend.query.reading;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.space.api.SpaceKind;
import yeobaek.backend.space.api.club.ClubMembershipApi;

@Component
@RequiredArgsConstructor
public class ClubReadingCandidateProvider implements ReadingCandidateProvider {

    private final ClubMembershipApi memberships;

    @Override
    public SpaceKind supportedKind() {
        return SpaceKind.CLUB;
    }

    @Override
    public List<Candidate> findCandidates(MemberId actorId) {
        return memberships.findJoinedByMember(actorId).stream()
                .map(membership -> new Candidate(membership.club(), membership.club().name()))
                .toList();
    }
}
