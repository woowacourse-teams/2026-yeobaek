package yeobaek.backend.comment.repository;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import yeobaek.backend.comment.domain.CommentView;

public interface CommentViewRepository extends JpaRepository<CommentView, Long> {

    @Query("select count(cv) > 0 from CommentView cv where cv.actorId = :memberId and cv.appreciationId = :commentId")
    boolean existsByMemberIdAndCommentId(@Param("memberId") Long memberId, @Param("commentId") Long commentId);

    @Query("""
            select distinct cv.appreciationId from CommentView cv
            where cv.actorId = :memberId and cv.appreciationId in :commentIds
            """)
    List<Long> findViewedCommentIds(@Param("memberId") Long memberId,
                                    @Param("commentIds") Collection<Long> commentIds);

    @Modifying
    @Query("delete from CommentView cv where cv.actorId = :memberId")
    void deleteAllByActorId(@Param("memberId") Long memberId);

    @Modifying
    @Query("delete from CommentView cv where cv.appreciationId = :appreciationId")
    void deleteAllByAppreciationId(@Param("appreciationId") Long appreciationId);

    @Modifying
    @Query("delete from CommentView cv where cv.appreciationId in :appreciationIds")
    void deleteAllByAppreciationIdIn(@Param("appreciationIds") Collection<Long> appreciationIds);

}
