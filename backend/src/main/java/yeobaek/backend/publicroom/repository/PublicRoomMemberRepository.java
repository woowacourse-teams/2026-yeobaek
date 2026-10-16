package yeobaek.backend.publicroom.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yeobaek.backend.publicroom.domain.PublicRoomMember;

public interface PublicRoomMemberRepository extends JpaRepository<PublicRoomMember, Long> {

    String MEMBER_ID = "memberId";

    Optional<PublicRoomMember> findByMemberIdAndPublicRoomId(Long memberId, Long publicRoomId);

    @Query("""
            select m from PublicRoomMember m
            left join fetch m.lastRead lr
            left join fetch lr.lastReadPassage
            where m.member.id = :memberId and m.publicRoom.id in :publicRoomIds
            """)
    List<PublicRoomMember> findAllByMemberIdAndPublicRoomIdIn(@Param(MEMBER_ID) Long memberId,
                                                              @Param("publicRoomIds") List<Long> publicRoomIds);

    @Query("""
            select m from PublicRoomMember m
            join fetch m.publicRoom r
            join fetch r.book b
            left join fetch m.lastRead lr
            left join fetch lr.lastReadPassage
            where m.member.id = :memberId and b.status = yeobaek.backend.book.domain.BookStatus.ACTIVE
              and m.lastVisitedAt is not null
            order by m.lastVisitedAt desc
            """)
    List<PublicRoomMember> findVisitedActiveByMemberId(@Param(MEMBER_ID) Long memberId);

    @Query("""
            select m from PublicRoomMember m
            join fetch m.publicRoom r
            join fetch r.book
            join fetch m.lastRead lr
            join fetch lr.lastReadPassage
            where m.member.id = :memberId and lr.lastReadAt is not null
            order by lr.lastReadAt desc
            """)
    List<PublicRoomMember> findReadingsByMemberId(@Param(MEMBER_ID) Long memberId);

    @Query("""
            select m.publicRoom.id as publicRoomId, count(m) as visitorCount
            from PublicRoomMember m
            where m.publicRoom.id in :publicRoomIds and m.lastVisitedAt is not null
            group by m.publicRoom.id
            """)
    List<PublicRoomVisitorCount> countVisitorsByPublicRoomIds(@Param("publicRoomIds") List<Long> publicRoomIds);

    @Modifying
    @Query("delete from PublicRoomMember m where m.member.id = :memberId")
    void deleteAllByMemberId(@Param(MEMBER_ID) Long memberId);
}
