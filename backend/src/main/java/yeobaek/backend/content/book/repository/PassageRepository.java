package yeobaek.backend.content.book.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yeobaek.backend.content.book.domain.Passage;

public interface PassageRepository extends JpaRepository<Passage, Long> {

    @Query("select p from Passage p where p.locationId = :locationId")
    Optional<Passage> findByLocationId(@Param("locationId") Long locationId);

    @Query("""
            select p.chapter.id as chapterId,
                   min(p.sequence.value) as startSequence,
                   max(p.sequence.value) as endSequence
            from Passage p
            where p.chapter.book.id = :bookId
            group by p.chapter.id
            """)
    List<ChapterPassageRange> findChapterRangesByBookId(@Param("bookId") Long bookId);

    @Query("""
            select distinct p from Passage p
            join fetch p.chapter
            join fetch p.sentences sentence
            where p.chapter.book.id = :bookId and p.sequence.value between :fromSequence and :toSequence
            order by p.sequence.value asc
            """)
    List<Passage> findRangeByBookId(@Param("bookId") Long bookId,
                                    @Param("fromSequence") int fromSequence,
                                    @Param("toSequence") int toSequence);

    @Query("select count(p) from Passage p where p.chapter.book.id = :bookId")
    long countByBookId(@Param("bookId") Long bookId);
}
