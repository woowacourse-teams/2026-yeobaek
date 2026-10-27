package yeobaek.backend.notification.comment;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import yeobaek.backend.member.domain.Member;

public interface CommentNotificationRecipientQueryRepository extends Repository<Member, Long> {

    @Query("""
            select cm.member.id from ClubMember cm
            join cm.lastRead lr
            join lr.lastReadPassage passage
            where cm.club.id = :clubId
              and cm.status = yeobaek.backend.club.domain.ClubMemberStatus.JOINED
              and cm.member.id <> :writerId
              and passage.sequence.value > :passageSequence
              and not exists (
                  select memberBlock.id from MemberBlock memberBlock
                  where memberBlock.blocker.id = cm.member.id
                    and memberBlock.blocked.id = :writerId
              )
            order by cm.id asc
            """)
    List<Long> findReaderIdsAheadInClub(
            @Param("clubId") Long clubId,
            @Param("passageSequence") int passageSequence,
            @Param("writerId") Long writerId);

    @Query("""
            select member.id
            from Member member
            where member.id <> :writerId
              and exists (
                  select participant.id
                  from PublicRoomMember participant
                  where participant.publicRoom.id = :publicRoomId
                    and participant.member.id = member.id
              )
              and exists (
                  select comment.id
                  from Comment comment
                  join comment.publicRoomComment publicRoomComment
                  where publicRoomComment.publicRoom.id = :publicRoomId
                    and comment.sentence.id = :sentenceId
                    and comment.writer.id = member.id
              )
              and not exists (
                  select memberBlock.id
                  from MemberBlock memberBlock
                  where memberBlock.blocker.id = member.id
                    and memberBlock.blocked.id = :writerId
              )
            """)
    List<Long> findPreviousCommenterIdsInPublicRoom(
            @Param("publicRoomId") Long publicRoomId,
            @Param("sentenceId") Long sentenceId,
            @Param("writerId") Long writerId
    );
}
