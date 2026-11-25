package yeobaek.backend.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import yeobaek.backend.book.domain.Author;
import yeobaek.backend.book.domain.AuthorBook;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.Chapter;
import yeobaek.backend.book.domain.vo.AuthorName;
import yeobaek.backend.book.domain.vo.BookDescription;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.book.domain.vo.BodyCharacterCount;
import yeobaek.backend.book.domain.vo.ChapterTitle;
import yeobaek.backend.book.repository.ActiveBookRepository;
import yeobaek.backend.book.repository.AuthorBookRepository;
import yeobaek.backend.book.repository.ChapterPassageRange;
import yeobaek.backend.book.repository.ChapterRepository;
import yeobaek.backend.book.repository.PassageRepository;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.NotFoundException;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private ActiveBookRepository bookRepository;
    @Mock
    private AuthorBookRepository authorBookRepository;
    @Mock
    private ChapterRepository chapterRepository;
    @Mock
    private PassageRepository passageRepository;
    @Mock
    private BookCoverUrlResolver bookCoverUrlResolver;
    @Mock
    private ChapterPassageRange chapterRange;
    @InjectMocks
    private BookService bookService;

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("검색어가 없으면 전체 도서를 작가와 함께 반환한다")
    void listWithoutKeyword(String keyword) {
        Book book = book(1L);
        given(bookRepository.findAll()).willReturn(List.of(book));
        given(authorBookRepository.findAllWithAuthorByBookIdIn(List.of(1L)))
                .willReturn(List.of(new AuthorBook(new Author(new AuthorName("현진건")), book)));

        var response = bookService.findBooks(keyword);

        assertThat(response.books()).singleElement().satisfies(summary -> {
            assertThat(summary.bookId()).isEqualTo(1L);
            assertThat(summary.authors()).containsExactly("현진건");
        });
    }

    @Test
    @DisplayName("검색 결과만 반환하고 작가 정보가 없으면 빈 배열을 제공한다")
    void searchResults() {
        given(bookRepository.searchByTitleOrAuthorName("운수")).willReturn(List.of(book(2L)));

        var response = bookService.findBooks("운수");

        assertThat(response.books()).singleElement().satisfies(summary -> {
            assertThat(summary.bookId()).isEqualTo(2L);
            assertThat(summary.authors()).isEmpty();
        });
    }

    @Test
    @DisplayName("목록과 상세에 표지 URL 및 장르와 태그를 보존한다")
    void exposeBookMetadata() {
        Book book = book(1L);
        String key = "123e4567-e89b-12d3-a456-426614174000";
        book.replaceCoverImage(key);
        book.replaceHashtags(List.of("독서", "모임", "독서"));
        given(bookRepository.findAll()).willReturn(List.of(book));
        given(bookRepository.getById(1L)).willReturn(book);
        given(bookCoverUrlResolver.resolve(key)).willReturn("https://covers.example/book.webp");

        var summary = bookService.findBooks(null).books().getFirst();
        var detail = bookService.findBook(1L);

        assertThat(List.of(summary.coverImageUrl(), detail.coverImageUrl()))
                .containsOnly("https://covers.example/book.webp");
        assertThat(List.of(summary.genres(), detail.genres())).containsOnly(List.of("소설"));
        assertThat(List.of(summary.hashtags(), detail.hashtags()))
                .allSatisfy(hashtags -> assertThat(hashtags).containsExactlyInAnyOrder("독서", "모임", "독서"));
    }

    @Test
    @DisplayName("상세는 소개와 환산 쪽수 및 목차별 본문 범위를 조합한다")
    void detailWithChapterRanges() {
        Book book = book(1L);
        Chapter first = new Chapter(book, new ChapterTitle("첫 장"), 1);
        Chapter empty = new Chapter(book, new ChapterTitle("빈 장"), 2);
        ReflectionTestUtils.setField(first, "id", 10L);
        ReflectionTestUtils.setField(empty, "id", 11L);
        given(bookRepository.getById(1L)).willReturn(book);
        given(chapterRepository.findAllByBookIdOrderBySequenceAsc(1L)).willReturn(List.of(first, empty));
        given(passageRepository.findChapterRangesByBookId(1L)).willReturn(List.of(chapterRange));
        given(chapterRange.getChapterId()).willReturn(10L);
        given(chapterRange.getStartSequence()).willReturn(1);
        given(chapterRange.getEndSequence()).willReturn(3);

        var detail = bookService.findBook(1L);

        assertThat(detail.description()).isEqualTo("책 소개");
        assertThat(detail.pageCount()).isEqualTo(2L);
        assertThat(detail.chapters()).extracting(chapter -> List.of(chapter.sequence(),
                        chapter.startPassageSequence(), chapter.endPassageSequence()))
                .containsExactly(List.of(1, 1, 3), List.of(2, 0, 0));
    }

    @Test
    @DisplayName("보완 전 도서의 선택 정보가 없어도 상세를 반환한다")
    void legacyOptionalMetadata() {
        Book book = new Book(new BookTitle("기존 도서"), null, null, 1, null, null, null, null, null);
        ReflectionTestUtils.setField(book, "id", 1L);
        given(bookRepository.getById(1L)).willReturn(book);

        var detail = bookService.findBook(1L);

        assertThat(detail.description()).isNull();
        assertThat(detail.pageCount()).isNull();
        assertThat(detail.coverImageUrl()).isNull();
        assertThat(detail.chapters()).isEmpty();
    }

    @Test
    @DisplayName("이용할 도서가 없으면 대상별 오류를 유지한다")
    void missingBook() {
        given(bookRepository.getById(99L)).willThrow(new NotFoundException(ErrorCode.BOOK_NOT_FOUND, "없는 도서"));

        assertThatThrownBy(() -> bookService.findBook(99L))
                .isInstanceOfSatisfying(NotFoundException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.BOOK_NOT_FOUND));
    }

    private Book book(Long id) {
        Book book = new Book(new BookTitle("운수 좋은 날"), null, 1924, 3, null,
                new BookDescription("책 소개"), new BodyCharacterCount(701), List.of("소설"), List.of("독서"));
        ReflectionTestUtils.setField(book, "id", id);
        return book;
    }
}
