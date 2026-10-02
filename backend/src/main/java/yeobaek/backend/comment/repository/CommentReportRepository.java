package yeobaek.backend.comment.repository;

import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import yeobaek.backend.comment.domain.CommentReport;

public interface CommentReportRepository extends JpaRepository<CommentReport, Long> {

    @Modifying
    @Query(value = "insert ignore into appreciation_reports (reporter_id, appreciation_id) "
            + "values (:reporterId, :commentId)", nativeQuery = true)
    int insertIfAbsent(@Param("reporterId") Long reporterId, @Param("commentId") Long commentId);

    @Modifying
    @Query("delete from CommentReport cr where cr.reporterId = :memberId")
    void deleteAllByReporterId(@Param("memberId") Long memberId);

    @Modifying
    @Query("delete from CommentReport cr where cr.appreciationId = :appreciationId")
    void deleteAllByAppreciationId(@Param("appreciationId") Long appreciationId);

    @Modifying
    @Query("delete from CommentReport cr where cr.appreciationId in :appreciationIds")
    void deleteAllByAppreciationIdIn(@Param("appreciationIds") Collection<Long> appreciationIds);

}
