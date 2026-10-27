package yeobaek.backend.notification.clubjoin;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import yeobaek.backend.member.domain.Member;

@FunctionalInterface
public interface ClubJoinNotificationRecipientQueryRepository extends Repository<Member, Long> {
    @Query("""
            select membership.member.id from ClubMember membership
            where membership.club.id = :clubId
              and membership.status = yeobaek.backend.club.domain.ClubMemberStatus.JOINED
              and membership.member.id <> :joiningMemberId
              and not exists (
                  select memberBlock.id from MemberBlock memberBlock
                  where memberBlock.blocker.id = membership.member.id
                    and memberBlock.blocked.id = :joiningMemberId
              )
            order by membership.id asc
            """)
    List<Long> findRecipientMemberIds(@Param("clubId") Long clubId,
                                      @Param("joiningMemberId") Long joiningMemberId);
}
