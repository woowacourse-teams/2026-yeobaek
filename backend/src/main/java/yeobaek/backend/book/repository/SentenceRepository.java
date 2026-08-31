package yeobaek.backend.book.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import yeobaek.backend.book.domain.Sentence;

public interface SentenceRepository extends JpaRepository<Sentence, Long> {
}
