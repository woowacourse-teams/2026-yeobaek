package watson.backend.book.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import watson.backend.book.domain.Book;

public interface BookRepository extends JpaRepository<Book, Long> {
}
