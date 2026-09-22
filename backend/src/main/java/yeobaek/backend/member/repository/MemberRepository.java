package yeobaek.backend.member.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yeobaek.backend.member.domain.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {

    @Query("""
            select m.id as memberId,
                   m.nickname.value as nickname,
                   count(cm.id) as clubCount
            from Member m
            left join ClubMember cm on cm.member = m
              and cm.status = yeobaek.backend.club.domain.ClubMemberStatus.JOINED
            group by m.id, m.nickname.value
            order by count(cm.id) desc, m.id asc
            """)
    List<AdminMemberDashboardStatistics> findAdminDashboardStatistics();

    @Query("select (count(m) > 0) from Member m where m.nickname.value = :nickname")
    boolean existsByNickname(@Param("nickname") String nickname);
}
