package yeobaek.backend.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import yeobaek.backend.member.domain.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByNickname(String nickname);
}
