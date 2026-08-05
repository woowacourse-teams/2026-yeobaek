package watson.backend.book;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorBookRepository extends JpaRepository<AuthorBook, Long> {
}
