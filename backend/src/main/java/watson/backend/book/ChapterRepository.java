package watson.backend.book;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {

    List<Chapter> findAllByBookIdOrderBySequenceAsc(Long bookId);
}
