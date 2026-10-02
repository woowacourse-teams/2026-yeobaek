package yeobaek.backend.collaboration.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppreciationContextRepository extends JpaRepository<AppreciationContextJpaEntity, Long> {

    @Modifying
    @Query("""
            delete from AppreciationContextJpaEntity ctx where exists (
              select a.id from AppreciationJpaEntity a
              where a.id = ctx.appreciationId and a.authorId = :memberId)
            """)
    void deleteAllByAuthorId(@Param("memberId") Long memberId);
}
