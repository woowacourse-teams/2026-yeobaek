package watson.backend.comment;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("""
            select c.passage.id as passageId, count(c) as commentCount
            from Comment c
            where c.clubMember.club.id = :clubId and c.passage.sequence between :fromSequence and :toSequence
            group by c.passage.id
            """)
    List<PassageCommentCount> countByClubIdAndSequenceRange(@Param("clubId") Long clubId,
                                                            @Param("fromSequence") int fromSequence,
                                                            @Param("toSequence") int toSequence);
}
