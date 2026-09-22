package yeobaek.backend.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yeobaek.backend.member.domain.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {

    @Query("select (count(m) > 0) from Member m where m.nickname.value = :nickname")
    boolean existsByNickname(@Param("nickname") String nickname);
}
