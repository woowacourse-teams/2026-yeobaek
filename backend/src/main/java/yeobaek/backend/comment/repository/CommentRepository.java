package yeobaek.backend.comment.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yeobaek.backend.comment.domain.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("""
            select c from Comment c
            join fetch c.clubMember cm
            join fetch cm.member
            where cm.club.id = :clubId and c.sentence.id = :sentenceId
              and not exists (
                  select mb.id from MemberBlock mb
                  where mb.blocker.id = :memberId and mb.blocked.id = cm.member.id
              )
            order by c.createdAt asc, c.id asc
            """)
    List<Comment> findAllVisibleWithWriterByClubIdAndSentenceId(@Param("memberId") Long memberId,
                                                                @Param("clubId") Long clubId,
                                                                @Param("sentenceId") Long sentenceId);

    @Query("""
            select c.sentence.id as sentenceId, count(c) as commentCount
            from Comment c
            where c.clubMember.club.id = :clubId and c.sentence.id in :sentenceIds
              and not exists (
                  select mb.id from MemberBlock mb
                  where mb.blocker.id = :memberId and mb.blocked.id = c.clubMember.member.id
              )
            group by c.sentence.id
            """)
    List<SentenceCommentCount> countVisibleByMemberIdAndClubIdAndSentenceIdIn(@Param("memberId") Long memberId,
                                                                              @Param("clubId") Long clubId,
                                                                              @Param("sentenceIds") List<Long> sentenceIds);
}
