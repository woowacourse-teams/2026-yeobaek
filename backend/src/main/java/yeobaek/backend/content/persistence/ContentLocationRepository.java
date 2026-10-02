package yeobaek.backend.content.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentLocationRepository extends JpaRepository<ContentLocationJpaEntity, Long> {
}
