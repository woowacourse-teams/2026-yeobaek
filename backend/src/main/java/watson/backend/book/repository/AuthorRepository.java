package watson.backend.book.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import watson.backend.book.domain.Author;

public interface AuthorRepository extends JpaRepository<Author, Long> {
}
