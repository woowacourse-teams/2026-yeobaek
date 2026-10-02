package yeobaek.backend.collaboration.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpaceContentBindingRepository
        extends JpaRepository<SpaceContentBindingJpaEntity, SpaceContentBindingId> {

    List<SpaceContentBindingJpaEntity> findAllBySpaceIdOrderByContentId(Long spaceId);
}
