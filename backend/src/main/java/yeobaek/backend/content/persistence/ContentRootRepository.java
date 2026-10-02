package yeobaek.backend.content.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentRootRepository extends JpaRepository<ContentJpaEntity, Long> {
}
