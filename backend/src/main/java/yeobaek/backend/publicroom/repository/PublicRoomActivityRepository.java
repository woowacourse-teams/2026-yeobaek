package yeobaek.backend.publicroom.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yeobaek.backend.publicroom.domain.PublicRoomActivity;

public interface PublicRoomActivityRepository extends JpaRepository<PublicRoomActivity, Long> {

    String MEMBER_ID = "memberId";

    Optional<PublicRoomActivity> findByMemberIdAndPublicRoomId(Long memberId, Long publicRoomId);

    @Query("""
            select a from PublicRoomActivity a
            where a.member.id = :memberId and a.publicRoom.id in :publicRoomIds
            """)
    List<PublicRoomActivity> findAllByMemberIdAndPublicRoomIdIn(@Param(MEMBER_ID) Long memberId,
                                                               @Param("publicRoomIds") List<Long> publicRoomIds);

    @Query("""
            select a from PublicRoomActivity a
            join fetch a.publicRoom r
            join fetch r.book b
            left join fetch a.lastReadPassage
            where a.member.id = :memberId and b.status = yeobaek.backend.book.domain.BookStatus.ACTIVE
              and a.lastVisitedAt is not null
            order by a.lastVisitedAt desc
            """)
    List<PublicRoomActivity> findVisitedActiveByMemberId(@Param(MEMBER_ID) Long memberId);

    @Query("""
            select a from PublicRoomActivity a
            join fetch a.publicRoom r
            join fetch r.book
            join fetch a.lastReadPassage
            where a.member.id = :memberId and a.lastReadAt is not null
            order by a.lastReadAt desc
            """)
    List<PublicRoomActivity> findReadingsByMemberId(@Param(MEMBER_ID) Long memberId);

    @Query("""
            select a.publicRoom.id as publicRoomId, count(a) as visitorCount
            from PublicRoomActivity a
            where a.publicRoom.id in :publicRoomIds and a.lastVisitedAt is not null
            group by a.publicRoom.id
            """)
    List<PublicRoomVisitorCount> countVisitorsByPublicRoomIds(@Param("publicRoomIds") List<Long> publicRoomIds);

    @Modifying
    @Query("delete from PublicRoomActivity a where a.member.id = :memberId")
    void deleteAllByMemberId(@Param(MEMBER_ID) Long memberId);
}
