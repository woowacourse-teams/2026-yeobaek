package yeobaek.backend.publicroom.repository;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yeobaek.backend.publicroom.domain.PublicRoom;

public interface PublicRoomRepository extends JpaRepository<PublicRoom, Long> {

    @Query("select r from PublicRoom r where r.spaceId = :spaceId")
    Optional<PublicRoom> findBySpaceRootId(@Param("spaceId") Long spaceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from PublicRoom r where r.id = :publicRoomId")
    Optional<PublicRoom> findByIdForUpdate(@Param("publicRoomId") Long publicRoomId);
}
