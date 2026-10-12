package yeobaek.backend.club.repository;

import jakarta.persistence.EntityManager;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.Session;
import org.hibernate.query.SelectionQuery;
import yeobaek.backend.club.domain.ClubMember;

@RequiredArgsConstructor
public class ClubMemberRepositoryCustomImpl implements ClubMemberRepositoryCustom {

    private final EntityManager entityManager;

    @Override
    public Optional<ClubMember> findJoinedWithLastReadByMemberIdAndClubIdForUpdate(Long memberId, Long clubId) {
        SelectionQuery<ClubMember> query = entityManager.unwrap(Session.class).createSelectionQuery("""
                select cm from ClubMember cm
                left join fetch cm.lastRead
                where cm.member.id = :memberId
                  and cm.club.id = :clubId
                  and cm.status = yeobaek.backend.club.domain.ClubMemberStatus.JOINED
                """, ClubMember.class);
        query.setParameter("memberId", memberId);
        query.setParameter("clubId", clubId);
        query.setHibernateLockMode(LockMode.PESSIMISTIC_WRITE);
        query.setLockScope(Locking.Scope.INCLUDE_FETCHES);
        return query.uniqueResultOptional();
    }
}
