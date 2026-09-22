package yeobaek.backend.club.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yeobaek.backend.club.domain.Club;

public interface ClubRepository extends JpaRepository<Club, Long> {

    @Query("""
            select c.id as clubId,
                   c.name.value as name,
                   b.id as bookId,
                   b.title.value as bookTitle,
                   b.status as bookStatus,
                   count(distinct case
                       when cm.status = yeobaek.backend.club.domain.ClubMemberStatus.JOINED then cm.id
                       else null
                   end) as memberCount,
                   count(distinct comment.id) as commentCount
            from Club c
            join c.book b
            left join ClubMember cm on cm.club = c
            left join Comment comment on comment.clubMember = cm
            group by c.id, c.name.value, b.id, b.title.value, b.status
            order by c.id asc
            """)
    List<AdminClubDashboardStatistics> findAdminDashboardStatistics();

    @Query("select (count(c) > 0) from Club c where c.joinCode.value = :joinCode")
    boolean existsByJoinCode(@Param("joinCode") String joinCode);

    @Query("select c from Club c where c.joinCode.value = :joinCode")
    Optional<Club> findByJoinCode(@Param("joinCode") String joinCode);
}
