package watson.backend.club;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClubMemberRepository extends JpaRepository<ClubMember, Long> {

    boolean existsByMemberIdAndClubId(Long memberId, Long clubId);

    @Query("""
            select cm from ClubMember cm
            join fetch cm.club c
            join fetch c.book
            left join fetch cm.lastReadPassage
            where cm.member.id = :memberId
            """)
    List<ClubMember> findAllWithClubAndBookByMemberId(@Param("memberId") Long memberId);

    @Query("""
            select cm.club.id as clubId, count(cm) as memberCount
            from ClubMember cm
            where cm.club.id in :clubIds
            group by cm.club.id
            """)
    List<ClubMemberCount> countByClubIds(@Param("clubIds") List<Long> clubIds);
}
