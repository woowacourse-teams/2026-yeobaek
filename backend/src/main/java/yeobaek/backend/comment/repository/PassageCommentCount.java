package yeobaek.backend.comment.repository;

/**
 * 모임 내 본문별 댓글 수 조회 결과 (인터페이스 프로젝션).
 */
public interface PassageCommentCount {

    Long getPassageId();

    long getCommentCount();
}
