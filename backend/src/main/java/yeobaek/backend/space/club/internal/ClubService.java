package yeobaek.backend.space.club.internal;

import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.RECOVERED;
import static yeobaek.backend.support.LogField.RESULT;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.space.api.SpaceKind;
import yeobaek.backend.space.api.club.ClubApi;
import yeobaek.backend.space.api.club.ClubName;
import yeobaek.backend.space.api.club.ClubResponse;
import yeobaek.backend.space.api.club.JoinCode;
import yeobaek.backend.space.api.club.JoinCodeAllocationException;
import yeobaek.backend.space.api.lifecycle.SpaceRootLifecycleApi;
import yeobaek.backend.space.club.persistence.Club;
import yeobaek.backend.space.club.repository.ClubRepository;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j(topic = "yeobaek.backend.web.v1.ClubService")
public class ClubService implements ClubApi {

    private static final int MAX_JOIN_CODE_ATTEMPTS = 5;

    private final ClubRepository clubs;
    private final SpaceRootLifecycleApi spaceRoots;

    @Override
    public ClubResponse create(ClubName name) {
        SpaceId spaceId = spaceRoots.create(SpaceKind.CLUB);
        var club = clubs.save(new Club(spaceId.value(), name, generateUniqueJoinCode()));
        return toResponse(club);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClubResponse> findById(Long clubId) {
        return clubs.findById(clubId).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClubResponse> findByJoinCode(JoinCode joinCode) {
        return clubs.findByJoinCode(joinCode.value()).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClubResponse> findBySpaceId(SpaceId spaceId) {
        return clubs.findBySpaceRootId(spaceId.value()).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClubResponse> findAll() {
        return clubs.findAllByOrderByIdAsc().stream().map(this::toResponse).toList();
    }

    private ClubResponse toResponse(Club club) {
        return new ClubSnapshot(new SpaceId(club.getSpaceId()), club.getId(), club.getName(), club.getJoinCode());
    }

    private JoinCode generateUniqueJoinCode() {
        for (int attempt = 0; attempt < MAX_JOIN_CODE_ATTEMPTS; attempt++) {
            JoinCode code = JoinCode.generate();
            if (!clubs.existsByJoinCode(code.value())) {
                if (attempt > 0) {
                    log.atWarn().addKeyValue(OPERATION, "club.generateUniqueJoinCode").addKeyValue(RESULT, RECOVERED)
                            .addKeyValue("retryCount", attempt).log("참여 코드 충돌을 재시도해 복구했습니다.");
                }
                return code;
            }
        }
        throw new JoinCodeAllocationException(
                "참여 코드 발급에 실패했습니다. 잠시 후 다시 시도해 주세요.",
                java.util.Map.of("attemptCount", Integer.toString(MAX_JOIN_CODE_ATTEMPTS)));
    }
}
