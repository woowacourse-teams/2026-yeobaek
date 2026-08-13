package yeobaek.backend.club.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import yeobaek.backend.club.domain.Club;

public interface ClubRepository extends JpaRepository<Club, Long> {

    boolean existsByJoinCode(String joinCode);

    Optional<Club> findByJoinCode(String joinCode);
}
