package yeobaek.backend.book.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yeobaek.backend.book.domain.Passage;

public interface PassageRepository extends JpaRepository<Passage, Long> {

    @Query("""
            select p.chapter.id as chapterId, min(p.sequence) as startSequence, max(p.sequence) as endSequence
            from Passage p
            where p.chapter.book.id = :bookId
            group by p.chapter.id
            """)
    List<ChapterPassageRange> findChapterRangesByBookId(@Param("bookId") Long bookId);

    @Query("""
            select distinct p from Passage p
            join fetch p.chapter
            join fetch p.sentences
            where p.chapter.book.id = :bookId and p.sequence between :fromSequence and :toSequence
            order by p.sequence asc
            """)
    List<Passage> findRangeByBookId(@Param("bookId") Long bookId,
                                    @Param("fromSequence") int fromSequence,
                                    @Param("toSequence") int toSequence);
}
