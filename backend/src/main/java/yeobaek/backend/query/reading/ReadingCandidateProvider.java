package yeobaek.backend.query.reading;

import java.util.List;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.space.api.Space;
import yeobaek.backend.space.api.SpaceKind;

public interface ReadingCandidateProvider {

    SpaceKind supportedKind();

    List<Candidate> findCandidates(MemberId actorId);

    record Candidate(Space space, String name) {
    }
}
