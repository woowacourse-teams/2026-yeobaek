package yeobaek.backend.club.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yeobaek.backend.club.domain.ClubMember;
import yeobaek.backend.club.domain.ClubMemberStatus;

public interface ClubMemberRepository extends JpaRepository<ClubMember, Long> {

    String MEMBER_ID = "memberId";

    @Modifying
    @Query("delete from ClubMember cm where cm.member.id = :memberId")
    void deleteAllByMemberId(@Param(MEMBER_ID) Long memberId);

    boolean existsByMemberIdAndClubIdAndStatus(Long memberId, Long clubId, ClubMemberStatus status);

    default boolean existsJoinedByMemberIdAndClubId(Long memberId, Long clubId) {
        return existsByMemberIdAndClubIdAndStatus(memberId, clubId, ClubMemberStatus.JOINED);
    }

    @Query("""
            select count(cm) > 0 from ClubMember cm
            where cm.member.id = :memberId
              and cm.status = yeobaek.backend.club.domain.ClubMemberStatus.JOINED
              and cm.club.id = (
                  select comment.clubMember.club.id from Comment comment where comment.id = :commentId
              )
            """)
    boolean existsJoinedByMemberIdAndCommentId(@Param(MEMBER_ID) Long memberId,
                                               @Param("commentId") Long commentId);

    Optional<ClubMember> findByMemberIdAndClubId(Long memberId, Long clubId);

    Optional<ClubMember> findByMemberIdAndClubIdAndStatus(Long memberId, Long clubId, ClubMemberStatus status);

    default Optional<ClubMember> findJoinedByMemberIdAndClubId(Long memberId, Long clubId) {
        return findByMemberIdAndClubIdAndStatus(memberId, clubId, ClubMemberStatus.JOINED);
    }

    @Query("""
            select cm from ClubMember cm
            join fetch cm.club c
            join fetch c.book
            join fetch cm.lastReadPassage
            where cm.member.id = :memberId
              and cm.status = yeobaek.backend.club.domain.ClubMemberStatus.JOINED
              and cm.lastReadAt is not null
            order by cm.lastReadAt desc
            """)
    List<ClubMember> findAllJoinedWithLastReadingByMemberId(@Param(MEMBER_ID) Long memberId);

    @Query("""
            select cm from ClubMember cm
            join fetch cm.club c
            join fetch c.book
            left join fetch cm.lastReadPassage
            where cm.member.id = :memberId
              and cm.status = yeobaek.backend.club.domain.ClubMemberStatus.JOINED
            """)
    List<ClubMember> findAllJoinedWithClubAndBookByMemberId(@Param(MEMBER_ID) Long memberId);

    @Query("""
            select cm.club.id as clubId, count(cm) as memberCount
            from ClubMember cm
            where cm.club.id in :clubIds
              and cm.status = yeobaek.backend.club.domain.ClubMemberStatus.JOINED
            group by cm.club.id
            """)
    List<ClubMemberCount> countJoinedByClubIds(@Param("clubIds") List<Long> clubIds);

    @Query("""
            select cm from ClubMember cm
            join fetch cm.member
            left join fetch cm.lastReadPassage
            where cm.club.id = :clubId
              and cm.status = yeobaek.backend.club.domain.ClubMemberStatus.JOINED
            order by cm.id asc
            """)
    List<ClubMember> findAllJoinedWithMemberByClubId(@Param("clubId") Long clubId);
}
