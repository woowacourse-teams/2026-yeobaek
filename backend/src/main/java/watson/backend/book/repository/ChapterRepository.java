package watson.backend.book.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import watson.backend.book.domain.Chapter;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {

    List<Chapter> findAllByBookIdOrderBySequenceAsc(Long bookId);
}
