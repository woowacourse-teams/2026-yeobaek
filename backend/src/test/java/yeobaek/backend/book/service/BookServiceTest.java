package yeobaek.backend.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import yeobaek.backend.book.domain.Author;
import yeobaek.backend.book.domain.AuthorBook;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.Chapter;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.dto.BookDetailResponse;
import yeobaek.backend.book.dto.BooksResponse;
import yeobaek.backend.book.dto.ChapterResponse;
import yeobaek.backend.book.repository.AuthorBookRepository;
import yeobaek.backend.book.repository.AuthorRepository;
import yeobaek.backend.book.repository.BookManagementRepository;
import yeobaek.backend.book.repository.ChapterRepository;
import yeobaek.backend.book.repository.PassageRepository;
import yeobaek.backend.support.NotFoundException;
import yeobaek.backend.support.IntegrationTest;
import yeobaek.backend.support.BadRequestException;
import yeobaek.backend.support.ErrorCode;

class BookServiceTest extends IntegrationTest {

    private static final String COVER_KEY = "yeobaek/book-covers/123e4567-e89b-12d3-a456-426614174000.webp";

    @Autowired
    private BookService bookService;

    @Autowired
    private BookManagementRepository bookRepository;

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

        BooksResponse response = bookService.findBooks(null);

        assertThat(response.books()).hasSize(1);
        assertThat(response.books().getFirst().authors()).containsExactly("현진건");
        assertThat(response.books().getFirst().passageCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("도서 목록과 상세는 표지 공개 URL을 동일하게 제공한다")
    void exposeCoverImageUrl() {
        Book book = bookRepository.save(new Book("표지 도서", null, null, 1, COVER_KEY));

        String expectedUrl = "https://yeobaek-local-book-covers.s3.ap-northeast-2.amazonaws.com/" + COVER_KEY;
        assertThat(bookService.findBooks(null).books().getFirst().coverImageUrl()).isEqualTo(expectedUrl);
        assertThat(bookService.findBook(book.getId()).coverImageUrl()).isEqualTo(expectedUrl);
    }

    @Test
    @DisplayName("키워드가 제목에 부분 일치하는 도서를 검색한다")
    void searchByTitle() {
        bookRepository.save(new Book("운수 좋은 날", null, 1924, 3));
        bookRepository.save(new Book("메밀꽃 필 무렵", null, 1936, 5));

        BooksResponse response = bookService.findBooks("운수");

        assertThat(response.books()).hasSize(1);
        assertThat(response.books().getFirst().title()).isEqualTo("운수 좋은 날");
    }

    @Test
    @DisplayName("키워드가 작가 이름에 부분 일치하는 도서를 검색한다")
    void searchByAuthorName() {
        Book matched = bookRepository.save(new Book("운수 좋은 날", null, 1924, 3));
        bookRepository.save(new Book("메밀꽃 필 무렵", null, 1936, 5));
        Author author = authorRepository.save(new Author("현진건"));
        authorBookRepository.save(new AuthorBook(author, matched));

        BooksResponse response = bookService.findBooks("현진");

        assertThat(response.books()).hasSize(1);
        assertThat(response.books().getFirst().title()).isEqualTo("운수 좋은 날");
    }

    @Test
    @DisplayName("제목과 작가 어디에도 일치하지 않으면 빈 목록을 반환한다")
    void searchNoMatch() {
        bookRepository.save(new Book("운수 좋은 날", null, 1924, 3));

        BooksResponse response = bookService.findBooks("이상");

        assertThat(response.books()).isEmpty();
    }

    @Test
    @DisplayName("공백 키워드는 전체 목록을 반환한다")
    void searchWithBlankKeyword() {
        bookRepository.save(new Book("운수 좋은 날", null, 1924, 3));
        bookRepository.save(new Book("메밀꽃 필 무렵", null, 1936, 5));

        BooksResponse response = bookService.findBooks(" ");

        assertThat(response.books()).hasSize(2);
    }

    @Test
    @DisplayName("도서 상세의 목차에 챕터별 본문 순서 범위가 계산된다")
    void findBookWithChapterRanges() {
        Book book = bookRepository.save(new Book("운수 좋은 날", null, 1924, 5));
        Chapter first = chapterRepository.save(new Chapter(book, "1장", 1));
        Chapter second = chapterRepository.save(new Chapter(book, "2장", 2));
        for (int sequence = 1; sequence <= 3; sequence++) {
            passageRepository.save(new Passage(first, sequence, "본문 " + sequence));
        }
        for (int sequence = 4; sequence <= 5; sequence++) {
            passageRepository.save(new Passage(second, sequence, "본문 " + sequence));
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

    @Test
    @DisplayName("삭제된 도서는 이용 가능한 도서 목록과 검색 결과에 나타나지 않는다")
    void excludesDeletedBookFromAvailableBooks() {
        Book book = bookRepository.save(new Book("운수 좋은 날", null, 1924, 1));
        bookRepository.delete(book.getId());

        assertThat(bookService.findBooks(null).books()).isEmpty();
        assertThat(bookService.findBooks("운수").books()).isEmpty();
    }

    @Test
    @DisplayName("삭제된 도서를 직접 조회하면 BOOK_NOT_AVAILABLE 오류가 발생한다")
    void cannotFindDeletedBookDetail() {
        Book book = bookRepository.save(new Book("운수 좋은 날", null, 1924, 1));
        bookRepository.delete(book.getId());

        assertThatThrownBy(() -> bookService.findBook(book.getId()))
                .isInstanceOf(BadRequestException.class)
                .extracting("code").isEqualTo(ErrorCode.BOOK_NOT_AVAILABLE);
    }
}
