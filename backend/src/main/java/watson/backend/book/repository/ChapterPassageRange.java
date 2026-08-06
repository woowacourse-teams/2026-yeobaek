package watson.backend.book.repository;

/**
 * 챕터별 본문 전체 순서 범위 조회 결과 (인터페이스 프로젝션).
 */
public interface ChapterPassageRange {

    Long getChapterId();

    int getStartSequence();

    int getEndSequence();
}
