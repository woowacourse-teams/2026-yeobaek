package yeobaek.backend.appreciation.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppreciationRepository extends JpaRepository<AppreciationJpaEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AppreciationJpaEntity a where a.id = :appreciationId")
    Optional<AppreciationJpaEntity> findByIdForUpdate(@Param("appreciationId") Long appreciationId);

    @Query("select a from AppreciationJpaEntity a where a.authorId = :memberId order by a.id")
    List<AppreciationJpaEntity> findAllByAuthorId(@Param("memberId") Long memberId);

    @Modifying
    @Query("delete from AppreciationJpaEntity a where a.authorId = :memberId")
    void deleteAllByAuthorId(@Param("memberId") Long memberId);
}
