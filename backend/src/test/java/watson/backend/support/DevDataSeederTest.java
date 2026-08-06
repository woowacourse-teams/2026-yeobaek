package watson.backend.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import watson.backend.book.domain.Book;
import watson.backend.book.domain.Passage;
import watson.backend.book.repository.AuthorBookRepository;
import watson.backend.book.repository.AuthorRepository;
import watson.backend.book.repository.BookRepository;
import watson.backend.book.repository.ChapterRepository;
import watson.backend.book.repository.PassageRepository;

class DevDataSeederTest extends RepositoryTest {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private AuthorBookRepository authorBookRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private PassageRepository passageRepository;

    @Test
    @DisplayName("시드는 본문 순서가 1..N으로 빈틈없고 도서의 본문 개수와 일치하도록 투입된다")
    void seedKeepsDenseSequenceContract() {
        new DevDataSeeder(bookRepository, authorRepository, authorBookRepository,
                chapterRepository, passageRepository).seed();

        Book book = bookRepository.findAll().getFirst();
        List<Integer> sequences = passageRepository.findAll().stream()
                .sorted(Comparator.comparing(Passage::getSequence))
                .map(Passage::getSequence)
                .toList();

        assertThat(sequences).hasSize(book.getPassageCount());
        assertThat(sequences.getFirst()).isEqualTo(1);
        assertThat(sequences).doesNotHaveDuplicates();
        assertThat(sequences.getLast()).isEqualTo(book.getPassageCount());
    }
}
