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
import yeobaek.backend.comment.dto.CommentResponse;

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
            select new yeobaek.backend.comment.dto.CommentResponse(
                c.id, writer.id, writer.nickname.value, c.content.value, c.createdAt, c.updatedAt,
                writer.id = :memberId,
                count(cl),
                sum(case when cl.member.id = :memberId then 1 else 0 end) > 0
            )
            from Comment c
            join c.writer writer
            join c.clubComment cc
            left join CommentLike cl on cl.comment.id = c.id
            where cc.club.id = :clubId and c.sentence.id = :sentenceId
              and not exists (
                  select mb.id from MemberBlock mb
                  where mb.blocker.id = :memberId and mb.blocked.id = writer.id
              )
            group by c.id, writer.id, writer.nickname.value, c.content.value, c.createdAt, c.updatedAt
            order by c.createdAt asc, c.id asc
            """)
    List<CommentResponse> findVisibleResponsesByClubIdAndSentenceId(@Param(MEMBER_ID) Long memberId,
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
            select new yeobaek.backend.comment.repository.CommentSentenceRow(
                c.id,
                c.createdAt,
                sentence.id,
                sentence.content.value,
                passage.id,
                passage.sequence.value,
                sentence.sequence.value
            )
            from Comment c
            join c.clubComment clubComment
            join c.sentence sentence
            join sentence.passage passage
            where clubComment.club.id = :clubId
              and not exists (
                  select memberBlock.id from MemberBlock memberBlock
                  where memberBlock.blocker.id = :memberId
                    and memberBlock.blocked.id = c.writer.id
              )
            """)
    List<CommentSentenceRow> findAllVisibleSentenceRowsByMemberIdAndClubId(
            @Param(MEMBER_ID) Long memberId,
            @Param(CLUB_ID) Long clubId);

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
            select new yeobaek.backend.comment.dto.CommentResponse(
                c.id, writer.id, writer.nickname.value, c.content.value, c.createdAt, c.updatedAt,
                writer.id = :memberId,
                count(cl),
                sum(case when cl.member.id = :memberId then 1 else 0 end) > 0
            )
            from Comment c
            join c.writer writer
            join c.publicRoomComment prc
            left join CommentLike cl on cl.comment.id = c.id
            where prc.publicRoom.id = :publicRoomId and c.sentence.id = :sentenceId
              and not exists (
                  select mb.id from MemberBlock mb
                  where mb.blocker.id = :memberId and mb.blocked.id = writer.id
              )
            group by c.id, writer.id, writer.nickname.value, c.content.value, c.createdAt, c.updatedAt
            order by c.createdAt asc, c.id asc
            """)
    List<CommentResponse> findVisibleResponsesInPublicRoom(@Param(MEMBER_ID) Long memberId,
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
            select c from Comment c
            join fetch c.publicRoomComment prc
            left join fetch c.clubComment
            join fetch c.writer
            join fetch c.sentence s
            join fetch s.passage
            where prc.publicRoom.id = :publicRoomId
            """)
    List<Comment> findAllWithSentenceByPublicRoomId(@Param(PUBLIC_ROOM_ID) Long publicRoomId);

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
