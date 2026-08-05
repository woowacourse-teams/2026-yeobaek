package watson.backend.comment;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("""
            select c from Comment c
            join fetch c.clubMember cm
            join fetch cm.member
            where cm.club.id = :clubId and c.passage.id = :passageId
            order by c.createdAt asc, c.id asc
            """)
    List<Comment> findAllWithWriterByClubIdAndPassageId(@Param("clubId") Long clubId,
                                                        @Param("passageId") Long passageId);

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
