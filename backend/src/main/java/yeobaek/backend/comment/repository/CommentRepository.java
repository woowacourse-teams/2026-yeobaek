package yeobaek.backend.comment.repository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yeobaek.backend.comment.domain.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    String MEMBER_ID = "memberId";
    String CLUB_ID = "clubId";
    String PUBLIC_ROOM_ID = "publicRoomId";

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Comment c where c.id = :commentId")
    Optional<Comment> findByIdForUpdate(@Param("commentId") Long commentId);

    @Query("""
            select c.clubComment.club.id as clubId, count(c) as commentCount
            from Comment c
            where c.clubComment.club.id in :clubIds
            group by c.clubComment.club.id
            """)
    List<ClubCommentCount> countCommentsByClubIds(@Param("clubIds") List<Long> clubIds);

    @Query("""
            select c from Comment c
            left join fetch c.clubComment cc
            left join fetch cc.club club
            left join fetch club.clubBook clubBook
            left join fetch clubBook.book
            left join fetch c.publicRoomComment prc
            left join fetch prc.publicRoom publicRoom
            left join fetch publicRoom.book
            join fetch c.writer writer
            where c.id = :commentId
              and not exists (
                  select mb.id from MemberBlock mb
                  where mb.blocker.id = :memberId
                    and mb.blocked.id = writer.id
              )
            """)
    Optional<Comment> findVisibleWithContextById(@Param(MEMBER_ID) Long memberId,
                                                 @Param("commentId") Long commentId);

    @Modifying
    @Query("""
            delete from Comment c
            where c.writer.id = :memberId
            """)
    void deleteAllByMemberId(@Param(MEMBER_ID) Long memberId);

    @Query("""
            select c from Comment c
            join fetch c.writer writer
            join c.clubComment cc
            where cc.club.id = :clubId and c.sentence.id = :sentenceId
              and not exists (
                  select mb.id from MemberBlock mb
                  where mb.blocker.id = :memberId and mb.blocked.id = writer.id
              )
            order by c.createdAt asc, c.id asc
            """)
    List<Comment> findAllVisibleWithWriterByClubIdAndSentenceId(@Param(MEMBER_ID) Long memberId,
                                                                @Param(CLUB_ID) Long clubId,
                                                                @Param("sentenceId") Long sentenceId);

    @Query("""
            select c.sentence.id as sentenceId, count(c) as commentCount
            from Comment c
            where c.clubComment.club.id = :clubId and c.sentence.id in :sentenceIds
              and not exists (
                  select mb.id from MemberBlock mb
                  where mb.blocker.id = :memberId and mb.blocked.id = c.writer.id
              )
            group by c.sentence.id
            """)
    List<SentenceCommentCount> countVisibleByMemberIdAndClubIdAndSentenceIdIn(@Param(MEMBER_ID) Long memberId,
                                                                              @Param(CLUB_ID) Long clubId,
                                                                              @Param("sentenceIds") List<Long> sentenceIds);

    @Query("""
            select c from Comment c
            join fetch c.clubComment cc
            left join fetch c.publicRoomComment
            join fetch c.writer
            join fetch c.sentence s
            join fetch s.passage
            where cc.club.id = :clubId
            """)
    List<Comment> findAllWithSentenceByClubId(@Param(CLUB_ID) Long clubId);

    @Query("""
            select count(c) from Comment c
            where c.clubComment.club.id = :clubId
              and c.sentence.passage.sequence.value <= :currentPassageSequence
              and not exists (
                  select cv.id from CommentView cv
                  where cv.member.id = :memberId and cv.comment.id = c.id
              )
              and not exists (
                  select mb.id from MemberBlock mb
                  where mb.blocker.id = :memberId and mb.blocked.id = c.writer.id
              )
            """)
    long countNewVisibleCommentsWithinProgress(@Param(MEMBER_ID) Long memberId,
                                               @Param(CLUB_ID) Long clubId,
                                               @Param("currentPassageSequence") int currentPassageSequence);

    @Query("""
            select c from Comment c
            join fetch c.writer writer
            join c.publicRoomComment prc
            where prc.publicRoom.id = :publicRoomId and c.sentence.id = :sentenceId
              and not exists (
                  select mb.id from MemberBlock mb
                  where mb.blocker.id = :memberId and mb.blocked.id = writer.id
              )
            order by c.createdAt asc, c.id asc
            """)
    List<Comment> findAllVisibleInPublicRoom(@Param(MEMBER_ID) Long memberId,
                                             @Param(PUBLIC_ROOM_ID) Long publicRoomId,
                                             @Param("sentenceId") Long sentenceId);

    @Query("""
            select c.sentence.id as sentenceId, count(c) as commentCount
            from Comment c
            where c.publicRoomComment.publicRoom.id = :publicRoomId and c.sentence.id in :sentenceIds
              and not exists (
                  select mb.id from MemberBlock mb
                  where mb.blocker.id = :memberId and mb.blocked.id = c.writer.id
              )
            group by c.sentence.id
            """)
    List<SentenceCommentCount> countVisibleInPublicRoom(@Param(MEMBER_ID) Long memberId,
                                                        @Param(PUBLIC_ROOM_ID) Long publicRoomId,
                                                        @Param("sentenceIds") List<Long> sentenceIds);

    @Query("""
            select s.id as sentenceId,
                   s.content.value as content,
                   p.id as passageId,
                   p.sequence.value as passageSequence,
                   s.sequence.value as sentenceSequence,
                   count(distinct c.id) as commentCount,
                   count(distinct case when cv.id is null then c.id else null end) as unreadCommentCount,
                   max(c.createdAt) as latestCommentCreatedAt
            from Comment c
            join c.sentence s
            join s.passage p
            left join CommentView cv on cv.comment.id = c.id and cv.member.id = :memberId
            where c.publicRoomComment.publicRoom.id = :publicRoomId
              and not exists (
                  select mb.id from MemberBlock mb
                  where mb.blocker.id = :memberId and mb.blocked.id = c.writer.id
              )
            group by s.id, s.content.value, p.id, p.sequence.value, s.sequence.value
            """)
    List<CommentedSentenceSummary> findPublicRoomCommentedSentenceSummaries(
            @Param(MEMBER_ID) Long memberId,
            @Param(PUBLIC_ROOM_ID) Long publicRoomId);

    @Query("""
            select count(c) from Comment c
            where c.publicRoomComment.publicRoom.id = :publicRoomId
              and c.sentence.passage.sequence.value <= :currentPassageSequence
              and not exists (
                  select cv.id from CommentView cv
                  where cv.member.id = :memberId and cv.comment.id = c.id
              )
              and not exists (
                  select mb.id from MemberBlock mb
                  where mb.blocker.id = :memberId and mb.blocked.id = c.writer.id
              )
            """)
    long countNewVisibleCommentsInPublicRoom(@Param(MEMBER_ID) Long memberId,
                                             @Param(PUBLIC_ROOM_ID) Long publicRoomId,
                                             @Param("currentPassageSequence") int currentPassageSequence);
}
