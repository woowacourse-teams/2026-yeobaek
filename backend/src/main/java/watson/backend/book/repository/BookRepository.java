package watson.backend.book.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import watson.backend.book.domain.Book;

public interface BookRepository extends JpaRepository<Book, Long> {

    @Query("""
            select b from Book b
            where b.title like concat('%', :keyword, '%')
               or exists (
                   select 1 from AuthorBook ab
                   where ab.book = b and ab.author.name like concat('%', :keyword, '%')
               )
            order by b.id
            """)
    List<Book> searchByTitleOrAuthorName(@Param("keyword") String keyword);
}
