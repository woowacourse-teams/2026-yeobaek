package yeobaek.backend.club.internal;

import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.RECOVERED;
import static yeobaek.backend.support.LogField.RESULT;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.club.api.ClubCommandApi;
import yeobaek.backend.club.api.ClubMembershipNotFoundFailure;
import yeobaek.backend.club.api.JoinCodeAllocationFailure;
import yeobaek.backend.club.domain.ClubMember;
import yeobaek.backend.club.domain.vo.ClubName;
import yeobaek.backend.club.domain.vo.JoinCode;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.club.repository.ClubRepository;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.foundation.identity.SpaceId;
import yeobaek.backend.space.api.SpaceNotFoundFailure;
import yeobaek.backend.space.api.SpaceRootLifecycleApi;
import yeobaek.backend.space.domain.Club;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j(topic = "yeobaek.backend.web.compatibility.ClubService")
public class ClubCommandService implements ClubCommandApi {

    private static final int MAX_JOIN_CODE_ATTEMPTS = 5;

    private final ClubRepository clubRepository;
    private final ClubMemberRepository memberRepository;
    private final SpaceRootLifecycleApi spaceRoots;

    @Override
    public Club create(ClubName name) {
        SpaceId spaceId = spaceRoots.create("CLUB");
        var club = clubRepository.save(new yeobaek.backend.club.domain.Club(
                spaceId.value(), name, generateUniqueJoinCode()));
        return new Club(spaceId, club.getId(), club.getName(), club.getJoinCode());
    }

    @Override
    public void join(MemberId actorId, SpaceId spaceId) {
        var club = findClub(spaceId);
        memberRepository.findByMemberIdAndClubId(actorId.value(), club.getId())
                .ifPresentOrElse(ClubMember::rejoin,
                        () -> memberRepository.save(new ClubMember(actorId, club)));
    }

    @Override
    public void leave(MemberId actorId, SpaceId spaceId) {
        var club = findClub(spaceId);
        memberRepository.findByMemberIdAndClubId(actorId.value(), club.getId())
                .orElseThrow(() -> new ClubMembershipNotFoundFailure(actorId, spaceId)).leave();
    }

    private yeobaek.backend.club.domain.Club findClub(SpaceId spaceId) {
        return clubRepository.findBySpaceRootId(spaceId.value())
                .orElseThrow(() -> new SpaceNotFoundFailure(spaceId));
    }

    private JoinCode generateUniqueJoinCode() {
        for (int attempt = 0; attempt < MAX_JOIN_CODE_ATTEMPTS; attempt++) {
            JoinCode code = JoinCode.generate();
            if (!clubRepository.existsByJoinCode(code.value())) {
                if (attempt > 0) {
                    log.atWarn().addKeyValue(OPERATION, "club.generateUniqueJoinCode").addKeyValue(RESULT, RECOVERED)
                            .addKeyValue("retryCount", attempt).log("참여 코드 충돌을 재시도해 복구했습니다.");
                }
                return code;
            }
        }
        throw new JoinCodeAllocationFailure();
    }
}
