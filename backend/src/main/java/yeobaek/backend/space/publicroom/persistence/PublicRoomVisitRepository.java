package yeobaek.backend.space.publicroom.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PublicRoomVisitRepository extends JpaRepository<PublicRoomVisitJpaEntity, Long> {

    String ACTOR_ID = "actorId";

    @Query("select v from PublicRoomVisitJpaEntity v where v.actorId = :actorId and v.spaceId = :spaceId")
    Optional<PublicRoomVisitJpaEntity> findOne(@Param(ACTOR_ID) Long actorId, @Param("spaceId") Long spaceId);

    @Query("""
            select v from PublicRoomVisitJpaEntity v
            where v.actorId = :actorId
            order by v.lastVisitedAt desc
            """)
    List<PublicRoomVisitJpaEntity> findAllByActorIdOrderByLastVisitedAtDesc(@Param(ACTOR_ID) Long actorId);

    @Query("""
            select v.spaceId as spaceId, count(v) as visitorCount
            from PublicRoomVisitJpaEntity v
            where v.spaceId in :spaceIds and v.lastVisitedAt is not null group by v.spaceId
            """)
    List<PublicRoomVisitorCountView> countVisitors(@Param("spaceIds") List<Long> spaceIds);

    @Modifying
    @Query("delete from PublicRoomVisitJpaEntity v where v.actorId = :actorId")
    void deleteAllByActorId(@Param(ACTOR_ID) Long actorId);
}
