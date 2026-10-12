package yeobaek.backend.club.repository;

import java.util.Optional;
import yeobaek.backend.club.domain.ClubMember;

@FunctionalInterface
public interface ClubMemberRepositoryCustom {

    Optional<ClubMember> findJoinedWithLastReadByMemberIdAndClubIdForUpdate(Long memberId, Long clubId);
}
