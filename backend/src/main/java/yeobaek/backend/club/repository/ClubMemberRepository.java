package yeobaek.backend.club.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yeobaek.backend.club.domain.ClubMember;
import yeobaek.backend.club.domain.ClubMemberStatus;

public interface ClubMemberRepository extends JpaRepository<ClubMember, Long> {

    boolean existsByMemberIdAndClubIdAndStatus(Long memberId, Long clubId, ClubMemberStatus status);

    default boolean existsActiveByMemberIdAndClubId(Long memberId, Long clubId) {
        return existsByMemberIdAndClubIdAndStatus(memberId, clubId, ClubMemberStatus.ACTIVE);
    }

    Optional<ClubMember> findByMemberIdAndClubId(Long memberId, Long clubId);

    Optional<ClubMember> findByMemberIdAndClubIdAndStatus(Long memberId, Long clubId, ClubMemberStatus status);

    default Optional<ClubMember> findActiveByMemberIdAndClubId(Long memberId, Long clubId) {
        return findByMemberIdAndClubIdAndStatus(memberId, clubId, ClubMemberStatus.ACTIVE);
    }

    @Query("""
            select cm from ClubMember cm
            join fetch cm.club c
            join fetch c.book
            join fetch cm.lastReadPassage
            where cm.member.id = :memberId
              and cm.status = yeobaek.backend.club.domain.ClubMemberStatus.ACTIVE
              and cm.lastReadAt is not null
            order by cm.lastReadAt desc
            """)
    List<ClubMember> findAllWithLastReadingByMemberId(@Param("memberId") Long memberId);

    @Query("""
            select cm from ClubMember cm
            join fetch cm.club c
            join fetch c.book
            left join fetch cm.lastReadPassage
            where cm.member.id = :memberId
              and cm.status = yeobaek.backend.club.domain.ClubMemberStatus.ACTIVE
            """)
    List<ClubMember> findAllWithClubAndBookByMemberId(@Param("memberId") Long memberId);

    @Query("""
            select cm.club.id as clubId, count(cm) as memberCount
            from ClubMember cm
            where cm.club.id in :clubIds
              and cm.status = yeobaek.backend.club.domain.ClubMemberStatus.ACTIVE
            group by cm.club.id
            """)
    List<ClubMemberCount> countByClubIds(@Param("clubIds") List<Long> clubIds);

    @Query("""
            select cm from ClubMember cm
            join fetch cm.member
            left join fetch cm.lastReadPassage
            where cm.club.id = :clubId
              and cm.status = yeobaek.backend.club.domain.ClubMemberStatus.ACTIVE
            order by cm.id asc
            """)
    List<ClubMember> findAllWithMemberByClubId(@Param("clubId") Long clubId);
}
