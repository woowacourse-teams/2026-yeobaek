package watson.backend.book;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthorBookRepository extends JpaRepository<AuthorBook, Long> {

    @Query("select ab from AuthorBook ab join fetch ab.author where ab.book.id in :bookIds")
    List<AuthorBook> findAllWithAuthorByBookIdIn(@Param("bookIds") List<Long> bookIds);
}
