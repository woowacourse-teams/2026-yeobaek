package yeobaek.backend.comment.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yeobaek.backend.comment.domain.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Modifying
    @Query("delete from Comment c where c.id in :appreciationIds")
    void deleteAllByAppreciationIdIn(@Param("appreciationIds") List<Long> appreciationIds);

    @Query("""
            select ctx.locationId as locationId, count(c) as commentCount
            from Comment c
            join AppreciationContextJpaEntity ctx on ctx.appreciationId = c.id
            where ctx.spaceId = :spaceId and ctx.locationId in :locationIds
            group by ctx.locationId
            """)
    List<LocationCommentCount> countBySpaceIdAndLocationIdIn(
            @Param("spaceId") Long spaceId,
            @Param("locationIds") List<Long> locationIds);

    @Query("""
            select ctx.locationId as locationId, count(c) as commentCount
            from Comment c join AppreciationJpaEntity a on a.id = c.id
            join AppreciationContextJpaEntity ctx on ctx.appreciationId = c.id
            where ctx.spaceId = :spaceId and ctx.locationId in :locationIds
              and a.authorId not in :excludedAuthorIds
            group by ctx.locationId
            """)
    List<LocationCommentCount> countExcludingAuthorsBySpaceIdAndLocationIdIn(
            @Param("spaceId") Long spaceId, @Param("locationIds") List<Long> locationIds,
            @Param("excludedAuthorIds") List<Long> excludedAuthorIds);
}
