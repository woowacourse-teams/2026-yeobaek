package yeobaek.backend.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import yeobaek.backend.admin.dto.AuthorEntryRequest;
import yeobaek.backend.admin.dto.BookUploadRequest;
import yeobaek.backend.admin.dto.BookUploadResponse;
import yeobaek.backend.admin.dto.ChapterUploadRequest;
import yeobaek.backend.admin.dto.PassageUploadRequest;
import yeobaek.backend.admin.dto.SentenceUploadRequest;
import yeobaek.backend.book.domain.Author;
import yeobaek.backend.book.domain.AuthorBook;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.repository.AuthorBookRepository;
import yeobaek.backend.book.repository.AuthorRepository;
import yeobaek.backend.book.repository.BookManagementRepository;
import yeobaek.backend.book.repository.ChapterRepository;
import yeobaek.backend.book.repository.PassageRepository;
import yeobaek.backend.support.BadRequestException;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.NotFoundException;
import yeobaek.backend.support.IntegrationTest;

class BookIngestServiceTest extends IntegrationTest {

    private static final String COVER_KEY = "yeobaek/book-covers/123e4567-e89b-12d3-a456-426614174000.png";

    @Autowired
    private BookIngestService bookIngestService;

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
    @DisplayName("업로드하면 본문 순서가 배열 순서대로 책 전체 기준 1..N으로 부여된다")
    void uploadAssignsDenseSequence() {
        BookUploadRequest request = new BookUploadRequest("운수 좋은 날", "자체 제작", 1924, null,
                List.of(new AuthorEntryRequest(null, "현진건", "0000 0001 2345 964X")),
                List.of(
                        new ChapterUploadRequest("1장", List.of(
                                passage("첫 문장. ", "둘째 문장."), passage("둘째 문단"))),
                        new ChapterUploadRequest("2장", List.of(passage("셋째 문단")))));

        BookUploadResponse response = bookIngestService.upload(request);

        assertThat(response.passageCount()).isEqualTo(3);
        Book book = bookRepository.findById(response.bookId()).orElseThrow();
        assertThat(book.getPassageCount()).isEqualTo(3);
        assertThat(chapterRepository.findAllByBookIdOrderBySequenceAsc(book.getId())).hasSize(2);
        List<Passage> passages = passageRepository.findAll();
        assertThat(passages).extracting(Passage::getSequence).containsExactlyInAnyOrder(1, 2, 3);
        Passage first = passageRepository.findRangeByBookId(book.getId(), 1, 1).getFirst();
        assertThat(first.getSentences()).extracting("sequence", "content")
                .containsExactly(tuple(1, "첫 문장. "), tuple(2, "둘째 문장."));
    }

    @Test
    @DisplayName("표지 키가 있으면 저장하고 공개 URL을 응답한다")
    void uploadWithCoverImage() {
        BookUploadRequest request = new BookUploadRequest("표지 도서", null, null, COVER_KEY,
                authorsOfUnknown(), chaptersWithOnePassage());

        BookUploadResponse response = bookIngestService.upload(request);

        assertThat(bookRepository.getById(response.bookId()).getCoverImageKey()).isEqualTo(COVER_KEY);
        assertThat(response.coverImageUrl()).isEqualTo(
                "https://yeobaek-local-book-covers.s3.ap-northeast-2.amazonaws.com/" + COVER_KEY);
    }

    @Test
    @DisplayName("ISNI가 기존 작가와 일치하면 재사용한다")
    void reuseAuthorByIsni() {
        Author existing = authorRepository.save(new Author("현진건", "000000012345964X"));

        BookUploadResponse response = bookIngestService.upload(requestWithAuthors(
                new AuthorEntryRequest(null, "현진건", "0000-0001-2345-964X")));

        List<AuthorBook> links = authorBookRepository.findAllWithAuthorByBookIdIn(List.of(response.bookId()));
        assertThat(links).hasSize(1);
        assertThat(links.getFirst().getAuthor().getId()).isEqualTo(existing.getId());
        assertThat(authorRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("authorId로 기존 작가를 참조할 수 있다")
    void referenceAuthorById() {
        Author existing = authorRepository.save(new Author("현진건"));

        BookUploadResponse response = bookIngestService.upload(requestWithAuthors(
                new AuthorEntryRequest(existing.getId(), null, null)));

        List<AuthorBook> links = authorBookRepository.findAllWithAuthorByBookIdIn(List.of(response.bookId()));
        assertThat(links.getFirst().getAuthor().getId()).isEqualTo(existing.getId());
    }

    @Test
    @DisplayName("존재하지 않는 authorId 참조는 AUTHOR_NOT_FOUND로 거부한다")
    void rejectUnknownAuthorId() {
        assertThatThrownBy(() -> bookIngestService.upload(requestWithAuthors(
                new AuthorEntryRequest(999L, null, null))))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("ISNI로 찾은 기존 작가와 이름이 다르면 AUTHOR_NAME_MISMATCH로 거부한다")
    void rejectNameMismatch() {
        authorRepository.save(new Author("현진건", "000000012345964X"));

        assertThatThrownBy(() -> bookIngestService.upload(requestWithAuthors(
                new AuthorEntryRequest(null, "이효석", "000000012345964X"))))
                .isInstanceOf(BadRequestException.class)
                .extracting("code").isEqualTo(ErrorCode.AUTHOR_NAME_MISMATCH);
    }

    @Test
    @DisplayName("한 업로드 안에 같은 작가를 중복 기재하면 DUPLICATE_AUTHOR로 거부한다")
    void rejectDuplicateAuthorEntry() {
        Author existing = authorRepository.save(new Author("현진건", "000000012345964X"));

        assertThatThrownBy(() -> bookIngestService.upload(requestWithAuthors(
                new AuthorEntryRequest(existing.getId(), null, null),
                new AuthorEntryRequest(null, "현진건", "000000012345964X"))))
                .isInstanceOf(BadRequestException.class)
                .extracting("code").isEqualTo(ErrorCode.DUPLICATE_AUTHOR);
    }

    @Test
    @DisplayName("제목·출판사·출판연도·작가 구성이 동일한 도서는 DUPLICATE_BOOK으로 거부한다")
    void rejectDuplicateBook() {
        Author author = authorRepository.save(new Author("현진건"));
        Book existing = bookRepository.save(new Book("운수 좋은 날", "자체 제작", 1924, 1));
        authorBookRepository.save(new AuthorBook(author, existing));

        BookUploadRequest request = new BookUploadRequest("운수 좋은 날", "자체 제작", 1924, null,
                List.of(new AuthorEntryRequest(author.getId(), null, null)),
                List.of(new ChapterUploadRequest("1장", List.of(passage("본문")))));

        assertThatThrownBy(() -> bookIngestService.upload(request))
                .isInstanceOf(BadRequestException.class)
                .extracting("code").isEqualTo(ErrorCode.DUPLICATE_BOOK);
    }

    @Test
    @DisplayName("출판연도가 다르면 같은 제목·작가라도 업로드를 허용한다")
    void allowSameTitleWithDifferentYear() {
        Author author = authorRepository.save(new Author("현진건"));
        Book existing = bookRepository.save(new Book("운수 좋은 날", "자체 제작", 1924, 1));
        authorBookRepository.save(new AuthorBook(author, existing));

        BookUploadRequest request = new BookUploadRequest("운수 좋은 날", "자체 제작", 1936, null,
                List.of(new AuthorEntryRequest(author.getId(), null, null)),
                List.of(new ChapterUploadRequest("1장", List.of(passage("본문")))));

        assertThat(bookIngestService.upload(request).bookId()).isNotNull();
    }

    @Test
    @DisplayName("삭제된 도서와 같은 서지·작가 구성의 도서는 새 ID로 다시 등록할 수 있다")
    void canRegisterBibliographicTwinOfDeletedBook() {
        Author author = authorRepository.save(new Author("현진건"));
        Book deleted = bookRepository.save(new Book("운수 좋은 날", "자체 제작", 1924, 1));
        authorBookRepository.save(new AuthorBook(author, deleted));
        bookRepository.delete(deleted.getId());
        BookUploadRequest request = new BookUploadRequest("운수 좋은 날", "자체 제작", 1924, null,
                List.of(new AuthorEntryRequest(author.getId(), null, null)),
                List.of(new ChapterUploadRequest("1장", List.of(passage("본문")))));

        BookUploadResponse response = bookIngestService.upload(request);

        assertThat(response.bookId()).isNotEqualTo(deleted.getId());
        assertThat(bookRepository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("작가 0명, 목차 0개, 본문 0개인 목차는 거부한다")
    void rejectEmptyStructures() {
        assertThatThrownBy(() -> bookIngestService.upload(new BookUploadRequest("제목", null, null, null,
                List.of(), chaptersWithOnePassage())))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> bookIngestService.upload(new BookUploadRequest("제목", null, null, null,
                authorsOfUnknown(), List.of())))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> bookIngestService.upload(new BookUploadRequest("제목", null, null, null,
                authorsOfUnknown(), List.of(new ChapterUploadRequest("1장", List.of())))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("문장이 없거나 문장 내용이 공백이면 거부한다")
    void rejectBlankContent() {
        assertThatThrownBy(() -> bookIngestService.upload(new BookUploadRequest("제목", null, null, null,
                authorsOfUnknown(), List.of(new ChapterUploadRequest("1장",
                        List.of(new PassageUploadRequest(List.of())))))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> bookIngestService.upload(new BookUploadRequest("제목", null, null, null,
                authorsOfUnknown(), List.of(new ChapterUploadRequest("1장",
                        List.of(passage(" ")))))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("문장 내용이 TEXT 저장 한도를 넘으면 거부한다")
    void rejectSentenceExceedingTextLimit() {
        BookUploadRequest request = new BookUploadRequest("제목", null, null, null,
                authorsOfUnknown(), List.of(new ChapterUploadRequest("1장",
                List.of(passage("a".repeat(65_536))))));

        assertThatThrownBy(() -> bookIngestService.upload(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("문장 하나는 65535바이트를 넘을 수 없습니다.");
    }

    @Test
    @DisplayName("authorId와 name을 함께 주는 작가 항목은 거부한다")
    void rejectAmbiguousAuthorEntry() {
        Author existing = authorRepository.save(new Author("현진건"));

        assertThatThrownBy(() -> bookIngestService.upload(requestWithAuthors(
                new AuthorEntryRequest(existing.getId(), "현진건", null))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private BookUploadRequest requestWithAuthors(AuthorEntryRequest... authors) {
        return new BookUploadRequest("새 책", null, null, null, List.of(authors), chaptersWithOnePassage());
    }

    private List<ChapterUploadRequest> chaptersWithOnePassage() {
        return List.of(new ChapterUploadRequest("1장", List.of(passage("본문"))));
    }

    private PassageUploadRequest passage(String... contents) {
        return new PassageUploadRequest(java.util.Arrays.stream(contents)
                .map(SentenceUploadRequest::new)
                .toList());
    }

    private List<AuthorEntryRequest> authorsOfUnknown() {
        return List.of(new AuthorEntryRequest(null, "작자 미상", null));
    }
}
