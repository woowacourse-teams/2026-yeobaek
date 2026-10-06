package yeobaek.backend.content.book.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import yeobaek.backend.content.book.domain.Sentence;

public interface SentenceRepository extends JpaRepository<Sentence, Long> {
}
