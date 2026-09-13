package yeobaek.backend.comment.repository;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yeobaek.backend.comment.domain.CommentView;

public interface CommentViewRepository extends JpaRepository<CommentView, Long> {

    boolean existsByMemberIdAndCommentId(Long memberId, Long commentId);

    @Query("""
            select distinct cv.comment.id from CommentView cv
            where cv.member.id = :memberId and cv.comment.id in :commentIds
            """)
    List<Long> findViewedCommentIds(@Param("memberId") Long memberId,
                                    @Param("commentIds") Collection<Long> commentIds);
}
