package yeobaek.backend.appreciation.comment.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yeobaek.backend.appreciation.comment.persistence.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Modifying
    @Query("delete from Comment c where c.id in :appreciationIds")
    void deleteAllByAppreciationIdIn(@Param("appreciationIds") List<Long> appreciationIds);

}
