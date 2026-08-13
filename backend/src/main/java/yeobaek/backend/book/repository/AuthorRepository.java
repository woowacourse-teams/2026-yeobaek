package yeobaek.backend.book.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import yeobaek.backend.book.domain.Author;

public interface AuthorRepository extends JpaRepository<Author, Long> {

    Optional<Author> findByIsni(String isni);

    List<Author> findAllByOrderByIdAsc();
}
