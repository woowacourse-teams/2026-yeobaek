package yeobaek.backend.club.repository;

/**
 * 모임별 회원 수 조회 결과 (인터페이스 프로젝션).
 */
public interface ClubMemberCount {

    Long getClubId();

    long getMemberCount();
}
