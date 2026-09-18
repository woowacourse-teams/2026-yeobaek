package yeobaek.backend.book.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yeobaek.backend.book.domain.Author;

public interface AuthorRepository extends JpaRepository<Author, Long> {

    @Query("select a from Author a where a.isni.value = :isni")
    Optional<Author> findByIsni(@Param("isni") String isni);

    List<Author> findAllByOrderByIdAsc();
}
