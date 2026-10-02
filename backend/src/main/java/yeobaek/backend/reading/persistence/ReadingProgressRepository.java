package yeobaek.backend.reading.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReadingProgressRepository extends JpaRepository<ReadingProgressJpaEntity, Long> {

    String ACTOR_ID = "actorId";

    @Query("""
            select p from ReadingProgressJpaEntity p
            where p.actorId = :actorId and p.spaceId = :spaceId and p.contentId = :contentId
            """)
    Optional<ReadingProgressJpaEntity> findOne(@Param(ACTOR_ID) Long actorId,
                                               @Param("spaceId") Long spaceId,
                                               @Param("contentId") Long contentId);

    @Modifying
    @Query("delete from ReadingProgressJpaEntity p where p.actorId = :actorId")
    void deleteAllByActorId(@Param(ACTOR_ID) Long actorId);
}
