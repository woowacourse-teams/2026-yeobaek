package watson.backend.club;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubRepository extends JpaRepository<Club, Long> {

    boolean existsByJoinCode(String joinCode);

    Optional<Club> findByJoinCode(String joinCode);
}
