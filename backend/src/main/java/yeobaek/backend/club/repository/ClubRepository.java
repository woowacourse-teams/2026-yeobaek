package yeobaek.backend.club.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yeobaek.backend.club.domain.Club;

public interface ClubRepository extends JpaRepository<Club, Long> {

    @Query("""
            select c from Club c
            join fetch c.book
            order by c.id asc
            """)
    List<Club> findAllWithBookByOrderByIdAsc();

    @Query("select (count(c) > 0) from Club c where c.joinCode.value = :joinCode")
    boolean existsByJoinCode(@Param("joinCode") String joinCode);

    @Query("select c from Club c where c.joinCode.value = :joinCode")
    Optional<Club> findByJoinCode(@Param("joinCode") String joinCode);
}
