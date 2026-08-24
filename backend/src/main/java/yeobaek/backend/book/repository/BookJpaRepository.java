package yeobaek.backend.book.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.BookStatus;

interface BookJpaRepository extends JpaRepository<Book, Long> {

    List<Book> findAllByStatusOrderByIdAsc(BookStatus status);

    @Query("""
            select b from Book b
            where b.status = :status
              and (b.title like concat('%', :keyword, '%')
               or exists (
                   select 1 from AuthorBook ab
                   where ab.book = b and ab.author.name like concat('%', :keyword, '%')
               ))
            order by b.id
            """)
    List<Book> searchActiveByTitleOrAuthorName(
            @Param("keyword") String keyword,
            @Param("status") BookStatus status
    );

    List<Book> findAllByTitleAndStatus(String title, BookStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Book b where b.id = :bookId")
    Optional<Book> findByIdForUpdate(@Param("bookId") Long bookId);
}
