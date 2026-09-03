package yeobaek.backend.comment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import yeobaek.backend.comment.domain.CommentReport;

public interface CommentReportRepository extends JpaRepository<CommentReport, Long> {

    boolean existsByReporterIdAndCommentId(Long reporterId, Long commentId);
}
