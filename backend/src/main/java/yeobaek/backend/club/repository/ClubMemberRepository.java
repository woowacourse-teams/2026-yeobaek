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
    @Query("delete from ClubMember cm where cm.memberId = :memberId")
    void deleteAllByMemberId(@Param(MEMBER_ID) Long memberId);

    boolean existsByMemberIdAndClubIdAndStatus(Long memberId, Long clubId, ClubMemberStatus status);

    default boolean existsJoinedByMemberIdAndClubId(Long memberId, Long clubId) {
        return existsByMemberIdAndClubIdAndStatus(memberId, clubId, ClubMemberStatus.JOINED);
    }

    @Query("""
            select count(cm) > 0 from ClubMember cm
            where cm.memberId = :memberId and cm.club.spaceId = :spaceId
              and cm.status = yeobaek.backend.club.domain.ClubMemberStatus.JOINED
            """)
    boolean existsJoinedByMemberIdAndSpaceId(@Param(MEMBER_ID) Long memberId,
                                             @Param("spaceId") Long spaceId);

    @Query("""
            select count(cm) > 0 from ClubMember cm
            join Comment comment on comment.id = :commentId
            where cm.memberId = :memberId
              and cm.status = yeobaek.backend.club.domain.ClubMemberStatus.JOINED
              and exists (select ctx.appreciationId from AppreciationContextJpaEntity ctx
                where ctx.appreciationId = comment.id and cm.club.spaceId = ctx.spaceId)
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
            where cm.memberId = :memberId
              and cm.status = yeobaek.backend.club.domain.ClubMemberStatus.JOINED
            """)
    List<ClubMember> findAllJoinedWithClubByMemberId(@Param(MEMBER_ID) Long memberId);

    @Query("""
            select cm.club.id as clubId, count(cm) as memberCount
            from ClubMember cm
            where cm.club.id in :clubIds
              and cm.status = yeobaek.backend.club.domain.ClubMemberStatus.JOINED
            group by cm.club.id
            """)
    List<ClubMemberCount> countJoinedMembersByClubIds(@Param("clubIds") List<Long> clubIds);

    @Query("""
            select cm.memberId as memberId, count(cm) as clubCount
            from ClubMember cm
            where cm.memberId in :memberIds
              and cm.status = yeobaek.backend.club.domain.ClubMemberStatus.JOINED
            group by cm.memberId
            """)
    List<MemberClubCount> countJoinedClubsByMemberIds(@Param("memberIds") List<Long> memberIds);

    @Query("""
            select cm from ClubMember cm
            where cm.club.id = :clubId
              and cm.status = yeobaek.backend.club.domain.ClubMemberStatus.JOINED
            order by cm.id asc
            """)
    List<ClubMember> findAllJoinedByClubId(@Param("clubId") Long clubId);
}
