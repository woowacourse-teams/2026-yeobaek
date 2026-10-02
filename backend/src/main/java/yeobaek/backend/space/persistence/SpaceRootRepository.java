package yeobaek.backend.space.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpaceRootRepository extends JpaRepository<SpaceJpaEntity, Long> {
}
