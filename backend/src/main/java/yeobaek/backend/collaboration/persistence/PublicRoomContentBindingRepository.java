package yeobaek.backend.collaboration.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicRoomContentBindingRepository
        extends JpaRepository<PublicRoomContentBindingJpaEntity, Long> {
}
