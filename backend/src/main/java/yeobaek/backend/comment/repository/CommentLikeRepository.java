package yeobaek.backend.comment.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yeobaek.backend.comment.domain.CommentLike;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    boolean existsByMemberIdAndCommentId(Long memberId, Long commentId);

    long countByCommentId(Long commentId);

    void deleteByMemberIdAndCommentId(Long memberId, Long commentId);

    @Query("""
            select new yeobaek.backend.comment.repository.CommentLikeSummary(
                cl.comment.id,
                count(cl),
                sum(case when cl.member.id = :memberId then 1 else 0 end) > 0
            )
            from CommentLike cl
            where cl.comment.id = :commentId
            group by cl.comment.id
            """)
    Optional<CommentLikeSummary> findSummaryByCommentId(@Param("memberId") Long memberId,
                                                        @Param("commentId") Long commentId);
}
