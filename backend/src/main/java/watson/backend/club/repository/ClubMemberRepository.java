package watson.backend.club.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import watson.backend.club.domain.ClubMember;

public interface ClubMemberRepository extends JpaRepository<ClubMember, Long> {

    boolean existsByMemberIdAndClubId(Long memberId, Long clubId);

    Optional<ClubMember> findByMemberIdAndClubId(Long memberId, Long clubId);

    @Query("""
            select cm from ClubMember cm
            join fetch cm.club c
            join fetch c.book
            join fetch cm.lastReadPassage
            where cm.member.id = :memberId and cm.lastReadAt is not null
            order by cm.lastReadAt desc
            """)
    List<ClubMember> findAllWithLastReadingByMemberId(@Param("memberId") Long memberId);

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

    @Query("""
            select cm from ClubMember cm
            join fetch cm.member
            left join fetch cm.lastReadPassage
            where cm.club.id = :clubId
            order by cm.id asc
            """)
    List<ClubMember> findAllWithMemberByClubId(@Param("clubId") Long clubId);
}
