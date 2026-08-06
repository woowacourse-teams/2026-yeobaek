package watson.backend.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import watson.backend.book.domain.Author;
import watson.backend.book.domain.AuthorBook;
import watson.backend.book.domain.Book;
import watson.backend.book.domain.Chapter;
import watson.backend.book.domain.Passage;
import watson.backend.book.dto.BookDetailResponse;
import watson.backend.book.dto.BooksResponse;
import watson.backend.book.dto.ChapterResponse;
import watson.backend.book.repository.AuthorBookRepository;
import watson.backend.book.repository.AuthorRepository;
import watson.backend.book.repository.BookRepository;
import watson.backend.book.repository.ChapterRepository;
import watson.backend.book.repository.PassageRepository;
import watson.backend.support.NotFoundException;
import watson.backend.support.RepositoryTest;

@Import(BookService.class)
class BookServiceTest extends RepositoryTest {

    @Autowired
    private BookService bookService;

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
    @DisplayName("도서 목록에 작가 이름이 함께 조회된다")
    void findBooksWithAuthors() {
        Book book = bookRepository.save(new Book("운수 좋은 날", "자체 제작", 1924, 3));
        Author author = authorRepository.save(new Author("현진건"));
        authorBookRepository.save(new AuthorBook(author, book));

        BooksResponse response = bookService.findBooks();

        assertThat(response.books()).hasSize(1);
        assertThat(response.books().getFirst().authors()).containsExactly("현진건");
        assertThat(response.books().getFirst().passageCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("도서 상세의 목차에 챕터별 본문 순서 범위가 계산된다")
    void findBookWithChapterRanges() {
        Book book = bookRepository.save(new Book("운수 좋은 날", null, 1924, 5));
        Chapter first = chapterRepository.save(new Chapter(book, "1장", 1));
        Chapter second = chapterRepository.save(new Chapter(book, "2장", 2));
        for (int sequence = 1; sequence <= 3; sequence++) {
            passageRepository.save(new Passage(first, sequence, "본문 " + sequence, null));
        }
        for (int sequence = 4; sequence <= 5; sequence++) {
            passageRepository.save(new Passage(second, sequence, "본문 " + sequence, null));
        }

        BookDetailResponse response = bookService.findBook(book.getId());

        assertThat(response.chapters()).extracting(ChapterResponse::startPassageSequence).containsExactly(1, 4);
        assertThat(response.chapters()).extracting(ChapterResponse::endPassageSequence).containsExactly(3, 5);
    }

    @Test
    @DisplayName("존재하지 않는 도서 상세 조회는 예외를 던진다")
    void findBookNotFound() {
        assertThatThrownBy(() -> bookService.findBook(999L))
                .isInstanceOf(NotFoundException.class);
    }
}
