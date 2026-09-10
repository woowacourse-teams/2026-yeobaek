package yeobaek.backend.comment.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yeobaek.backend.comment.domain.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    String MEMBER_ID = "memberId";
    String CLUB_ID = "clubId";

    @Query("""
            select c from Comment c
            join fetch c.clubMember cm
            join fetch cm.club club
            join fetch club.book
            where c.id = :commentId
              and not exists (
                  select mb.id from MemberBlock mb
                  where mb.blocker.id = :memberId and mb.blocked.id = cm.member.id
              )
            """)
    Optional<Comment> findVisibleWithContextById(@Param(MEMBER_ID) Long memberId,
                                                 @Param("commentId") Long commentId);

    @Modifying
    @Query("""
            delete from Comment c
            where c.clubMember.id in (
                select cm.id from ClubMember cm where cm.member.id = :memberId
            )
            """)
    void deleteAllByMemberId(@Param(MEMBER_ID) Long memberId);

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
    List<Comment> findAllVisibleWithWriterByClubIdAndSentenceId(@Param(MEMBER_ID) Long memberId,
                                                                @Param(CLUB_ID) Long clubId,
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
    List<SentenceCommentCount> countVisibleByMemberIdAndClubIdAndSentenceIdIn(@Param(MEMBER_ID) Long memberId,
                                                                              @Param(CLUB_ID) Long clubId,
                                                                              @Param("sentenceIds") List<Long> sentenceIds);

    @Query("""
            select s.id as sentenceId,
                   s.content as content,
                   p.id as passageId,
                   p.sequence as passageSequence,
                   s.sequence as sentenceSequence,
                   count(distinct c.id) as commentCount,
                   count(distinct case when cv.id is null then c.id else null end) as unreadCommentCount,
                   max(c.createdAt) as latestCommentCreatedAt
            from Comment c
            join c.sentence s
            join s.passage p
            left join CommentView cv on cv.comment.id = c.id and cv.member.id = :memberId
            where c.clubMember.club.id = :clubId
              and not exists (
                  select mb.id from MemberBlock mb
                  where mb.blocker.id = :memberId and mb.blocked.id = c.clubMember.member.id
              )
            group by s.id, s.content, p.id, p.sequence, s.sequence
            """)
    List<CommentedSentenceSummary> findCommentedSentenceSummaries(@Param(MEMBER_ID) Long memberId,
                                                                  @Param(CLUB_ID) Long clubId);

    @Query("""
            select count(c) from Comment c
            where c.clubMember.club.id = :clubId
              and c.sentence.passage.sequence <= :currentPassageSequence
              and not exists (
                  select cv.id from CommentView cv
                  where cv.member.id = :memberId and cv.comment.id = c.id
              )
              and not exists (
                  select mb.id from MemberBlock mb
                  where mb.blocker.id = :memberId and mb.blocked.id = c.clubMember.member.id
              )
            """)
    long countNewVisibleCommentsWithinProgress(@Param(MEMBER_ID) Long memberId,
                                               @Param(CLUB_ID) Long clubId,
                                               @Param("currentPassageSequence") int currentPassageSequence);
}
