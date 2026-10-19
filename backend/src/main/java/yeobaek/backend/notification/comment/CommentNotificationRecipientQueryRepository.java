package yeobaek.backend.notification.comment;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import yeobaek.backend.notification.registration.domain.NotificationRegistration;

@FunctionalInterface
public interface CommentNotificationRecipientQueryRepository extends Repository<NotificationRegistration, Long> {

    @Query("""
            select distinct registration.firebaseInstallationId.value
            from NotificationRegistration registration
            where registration.member.id <> :writerId
              and exists (
                  select participant.id
                  from PublicRoomMember participant
                  where participant.publicRoom.id = :publicRoomId
                    and participant.member.id = registration.member.id
              )
              and exists (
                  select comment.id
                  from Comment comment
                  join comment.publicRoomComment publicRoomComment
                  where publicRoomComment.publicRoom.id = :publicRoomId
                    and comment.sentence.id = :sentenceId
                    and comment.writer.id = registration.member.id
              )
              and not exists (
                  select memberBlock.id
                  from MemberBlock memberBlock
                  where memberBlock.blocker.id = registration.member.id
                    and memberBlock.blocked.id = :writerId
              )
            """)
    List<String> findFidsForPublicRoomComment(
            @Param("publicRoomId") Long publicRoomId,
            @Param("sentenceId") Long sentenceId,
            @Param("writerId") Long writerId
    );
}
