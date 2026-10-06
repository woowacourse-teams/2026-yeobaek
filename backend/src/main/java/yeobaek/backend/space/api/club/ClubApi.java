package yeobaek.backend.space.api.club;

import java.util.List;
import java.util.Optional;
import yeobaek.backend.shared.identity.SpaceId;

public interface ClubApi {

    ClubResponse create(ClubName name);

    Optional<ClubResponse> findById(Long clubId);

    Optional<ClubResponse> findByJoinCode(JoinCode joinCode);

    Optional<ClubResponse> findBySpaceId(SpaceId spaceId);

    List<ClubResponse> findAll();
}
