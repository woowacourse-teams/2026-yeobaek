package watson.backend.club;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubMemberRepository extends JpaRepository<ClubMember, Long> {

    boolean existsByMemberIdAndClubId(Long memberId, Long clubId);
}
