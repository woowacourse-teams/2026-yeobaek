package yeobaek.backend.member.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yeobaek.backend.member.domain.MemberBlock;

public interface MemberBlockRepository extends JpaRepository<MemberBlock, Long> {

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    void deleteByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    @Query("""
            select mb from MemberBlock mb
            join fetch mb.blocked blocked
            where mb.blocker.id = :blockerId
            order by blocked.nickname asc, blocked.id asc
            """)
    List<MemberBlock> findAllWithBlockedByBlockerId(@Param("blockerId") Long blockerId);

    @Query("""
            select mb.blocked.id from MemberBlock mb
            where mb.blocker.id = :blockerId and mb.blocked.id in :memberIds
            """)
    List<Long> findBlockedMemberIds(@Param("blockerId") Long blockerId,
                                    @Param("memberIds") List<Long> memberIds);
}
