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
import yeobaek.backend.content.api.value.AuthorName;
import yeobaek.backend.content.api.value.BookTitle;
import yeobaek.backend.content.api.value.ChapterTitle;
import yeobaek.backend.content.api.value.Isni;
import yeobaek.backend.content.api.value.Publisher;
import yeobaek.backend.content.api.value.SentenceContent;
import yeobaek.backend.content.api.book.BookIngestException;
import yeobaek.backend.content.book.domain.Author;
import yeobaek.backend.content.book.domain.AuthorBook;
import yeobaek.backend.content.book.persistence.Book;
import yeobaek.backend.content.book.domain.Passage;
import yeobaek.backend.content.book.domain.vo.ContentSequence;
import yeobaek.backend.content.book.domain.vo.PassageCount;
import yeobaek.backend.content.book.repository.AuthorBookRepository;
import yeobaek.backend.content.book.repository.AuthorRepository;
import yeobaek.backend.content.book.repository.BookManagementRepository;
import yeobaek.backend.content.book.repository.ChapterRepository;
import yeobaek.backend.content.book.repository.PassageRepository;
import yeobaek.backend.collaboration.api.binding.SpaceContentBindingApi;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.support.IntegrationTest;
import yeobaek.backend.space.publicroom.repository.PublicRoomRepository;

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

    @Autowired
    private PublicRoomRepository publicRoomRepository;

    @Autowired
    private SpaceContentBindingApi bindingApi;

    @Test
    @DisplayName("업로드하면 본문 순서가 배열 순서대로 책 전체 기준 1..N으로 부여된다")
    void uploadAssignsDenseSequence() {
        BookUploadRequest request = new BookUploadRequest(new BookTitle("운수 좋은 날"), new Publisher("자체 제작"), 1924, null,
                List.of(new AuthorEntryRequest(null, new AuthorName("현진건"), new Isni("0000 0001 2345 964X"))),
                List.of(
                        new ChapterUploadRequest(new ChapterTitle("1장"), List.of(
                                passage("첫 문장. ", "둘째 문장."), passage("둘째 문단"))),
                        new ChapterUploadRequest(new ChapterTitle("2장"), List.of(passage("셋째 문단")))));

        BookUploadResponse response = bookIngestService.upload(request);

        assertThat(response.passageCount()).isEqualTo(3);
        Book book = bookRepository.findById(response.bookId()).orElseThrow();
        assertThat(book.getPassageCount()).isEqualTo(new PassageCount(3));
        assertThat(chapterRepository.findAllByBookIdOrderBySequenceAsc(book.getId())).hasSize(2);
        List<Passage> passages = passageRepository.findAll();
        assertThat(passages).extracting(Passage::getSequence)
                .containsExactlyInAnyOrder(new ContentSequence(1), new ContentSequence(2), new ContentSequence(3));
        Passage first = passageRepository.findRangeByBookId(book.getId(), 1, 1).getFirst();
        assertThat(first.getSentences()).extracting("sequence", "content")
                .containsExactly(tuple(new ContentSequence(1), "첫 문장. "),
                        tuple(new ContentSequence(2), "둘째 문장."));
    }

    @Test
    @DisplayName("도서를 업로드하면 같은 트랜잭션에서 공개방을 하나 생성한다")
    void uploadCreatesPublicRoom() {
        BookUploadResponse response = bookIngestService.upload(new BookUploadRequest(
                new BookTitle("공개방 도서"), null, null, null,
                authorsOfUnknown(), chaptersWithOnePassage()));

        var room = publicRoomRepository.findAll().getFirst();
        Book book = bookRepository.getById(response.bookId());
        assertThat(bindingApi.isBound(new SpaceId(room.getSpaceId()),
                new ContentId(book.getContentId()))).isTrue();
    }

    @Test
    @DisplayName("표지 키가 있으면 저장하고 공개 URL을 응답한다")
    void uploadWithCoverImage() {
        BookUploadRequest request = new BookUploadRequest(new BookTitle("표지 도서"), null, null, COVER_KEY,
                authorsOfUnknown(), chaptersWithOnePassage());

        BookUploadResponse response = bookIngestService.upload(request);

        assertThat(bookRepository.getById(response.bookId()).getCoverImageKey()).isEqualTo(COVER_KEY);
        assertThat(response.coverImageUrl()).isEqualTo(
                "https://yeobaek-local-book-covers.s3.ap-northeast-2.amazonaws.com/" + COVER_KEY);
    }

    @Test
    @DisplayName("ISNI가 기존 작가와 일치하면 재사용한다")
    void reuseAuthorByIsni() {
        Author existing = authorRepository.save(new Author(new AuthorName("현진건"), new Isni("000000012345964X")));

        BookUploadResponse response = bookIngestService.upload(requestWithAuthors(
                new AuthorEntryRequest(null, new AuthorName("현진건"), new Isni("0000-0001-2345-964X"))));

        List<AuthorBook> links = authorBookRepository.findAllWithAuthorByBookIdIn(List.of(response.bookId()));
        assertThat(links).hasSize(1);
        assertThat(links.getFirst().getAuthor().getId()).isEqualTo(existing.getId());
        assertThat(authorRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("authorId로 기존 작가를 참조할 수 있다")
    void referenceAuthorById() {
        Author existing = authorRepository.save(new Author(new AuthorName("현진건")));

        BookUploadResponse response = bookIngestService.upload(requestWithAuthors(
                new AuthorEntryRequest(existing.getId(), null, null)));

        List<AuthorBook> links = authorBookRepository.findAllWithAuthorByBookIdIn(List.of(response.bookId()));
        assertThat(links.getFirst().getAuthor().getId()).isEqualTo(existing.getId());
    }

    @Test
    @DisplayName("존재하지 않는 authorId 참조는 AUTHOR_NOT_FOUND로 거부한다")
    void rejectUnknownAuthorId() {
        assertIngestFailure(() -> bookIngestService.upload(requestWithAuthors(
                        new AuthorEntryRequest(999L, null, null))), ErrorCode.AUTHOR_NOT_FOUND,
                "authorId가 가리키는 작가가 존재하지 않습니다: authorId=999", "AUTHOR_NOT_FOUND");
    }

    @Test
    @DisplayName("ISNI로 찾은 기존 작가와 이름이 다르면 AUTHOR_NAME_MISMATCH로 거부한다")
    void rejectNameMismatch() {
        authorRepository.save(new Author(new AuthorName("현진건"), new Isni("000000012345964X")));

        assertIngestFailure(() -> bookIngestService.upload(requestWithAuthors(
                        new AuthorEntryRequest(null, new AuthorName("이효석"), new Isni("000000012345964X")))),
                ErrorCode.AUTHOR_NAME_MISMATCH,
                "ISNI로 찾은 기존 작가와 요청한 작가 이름이 일치하지 않습니다.", "AUTHOR_NAME_MISMATCH");
    }

    @Test
    @DisplayName("한 업로드 안에 같은 작가를 중복 기재하면 DUPLICATE_AUTHOR로 거부한다")
    void rejectDuplicateAuthorEntry() {
        Author existing = authorRepository.save(new Author(new AuthorName("현진건"), new Isni("000000012345964X")));

        assertIngestFailure(() -> bookIngestService.upload(requestWithAuthors(
                new AuthorEntryRequest(existing.getId(), null, null),
                new AuthorEntryRequest(null, new AuthorName("현진건"), new Isni("000000012345964X")))),
                ErrorCode.DUPLICATE_AUTHOR,
                "한 업로드 요청에 같은 작가가 중복 기재되었습니다: authorId=" + existing.getId(),
                "DUPLICATE_AUTHOR");
    }

    @Test
    @DisplayName("제목·출판사·출판연도·작가 구성이 동일한 도서는 DUPLICATE_BOOK으로 거부한다")
    void rejectDuplicateBook() {
        Author author = authorRepository.save(new Author(new AuthorName("현진건")));
        Book existing = bookRepository.save(newBook(new BookTitle("운수 좋은 날"), new Publisher("자체 제작"), 1924, 1, null));
        authorBookRepository.save(new AuthorBook(author, existing));

        BookUploadRequest request = new BookUploadRequest(new BookTitle("운수 좋은 날"), new Publisher("자체 제작"), 1924, null,
                List.of(new AuthorEntryRequest(author.getId(), null, null)),
                List.of(new ChapterUploadRequest(new ChapterTitle("1장"), List.of(passage("본문")))));

        assertIngestFailure(() -> bookIngestService.upload(request), ErrorCode.DUPLICATE_BOOK,
                "동일한 서지 정보와 작가 구성의 활성 도서가 이미 존재합니다.", "DUPLICATE_BOOK");
    }

    @Test
    @DisplayName("공동 작가 순서가 달라도 구성이 같으면 DUPLICATE_BOOK으로 거부한다")
    void rejectDuplicateBookWithReorderedAuthors() {
        Author first = authorRepository.save(new Author(new AuthorName("현진건")));
        Author second = authorRepository.save(new Author(new AuthorName("이효석")));
        Book existing = bookRepository.save(newBook(new BookTitle("공동 작품"), null, null, 1, null));
        authorBookRepository.save(new AuthorBook(first, existing));
        authorBookRepository.save(new AuthorBook(second, existing));

        BookUploadRequest request = new BookUploadRequest(new BookTitle("공동 작품"), null, null, null,
                List.of(
                        new AuthorEntryRequest(second.getId(), null, null),
                        new AuthorEntryRequest(first.getId(), null, null)),
                chaptersWithOnePassage());

        assertIngestFailure(() -> bookIngestService.upload(request), ErrorCode.DUPLICATE_BOOK,
                "동일한 서지 정보와 작가 구성의 활성 도서가 이미 존재합니다.", "DUPLICATE_BOOK");
    }

    @Test
    @DisplayName("제목·출판사·출판연도가 같아도 작가 구성이 다르면 업로드를 허용한다")
    void allowUploadWithDifferentAuthors() {
        Author existingAuthor = authorRepository.save(new Author(new AuthorName("현진건")));
        Author otherAuthor = authorRepository.save(new Author(new AuthorName("이효석")));
        Book existing = bookRepository.save(newBook(new BookTitle("운수 좋은 날"), new Publisher("자체 제작"), 1924, 1, null));
        authorBookRepository.save(new AuthorBook(existingAuthor, existing));

        BookUploadRequest request = new BookUploadRequest(new BookTitle("운수 좋은 날"), new Publisher("자체 제작"), 1924, null,
                List.of(new AuthorEntryRequest(otherAuthor.getId(), null, null)),
                chaptersWithOnePassage());

        assertThat(bookIngestService.upload(request).bookId()).isNotNull();
    }

    @Test
    @DisplayName("제목·출판사·출판연도가 같아도 새 작가가 포함되면 업로드를 허용한다")
    void allowUploadWithNewAuthor() {
        Author existingAuthor = authorRepository.save(new Author(new AuthorName("현진건")));
        Book existing = bookRepository.save(newBook(new BookTitle("운수 좋은 날"), new Publisher("자체 제작"), 1924, 1, null));
        authorBookRepository.save(new AuthorBook(existingAuthor, existing));

        BookUploadRequest request = new BookUploadRequest(new BookTitle("운수 좋은 날"), new Publisher("자체 제작"), 1924, null,
                List.of(new AuthorEntryRequest(null, new AuthorName("새 작가"), null)),
                chaptersWithOnePassage());

        assertThat(bookIngestService.upload(request).bookId()).isNotNull();
    }

    @Test
    @DisplayName("출판연도가 다르면 같은 제목·작가라도 업로드를 허용한다")
    void allowSameTitleWithDifferentYear() {
        Author author = authorRepository.save(new Author(new AuthorName("현진건")));
        Book existing = bookRepository.save(newBook(new BookTitle("운수 좋은 날"), new Publisher("자체 제작"), 1924, 1, null));
        authorBookRepository.save(new AuthorBook(author, existing));

        BookUploadRequest request = new BookUploadRequest(new BookTitle("운수 좋은 날"), new Publisher("자체 제작"), 1936, null,
                List.of(new AuthorEntryRequest(author.getId(), null, null)),
                List.of(new ChapterUploadRequest(new ChapterTitle("1장"), List.of(passage("본문")))));

        assertThat(bookIngestService.upload(request).bookId()).isNotNull();
    }

    @Test
    @DisplayName("삭제된 도서와 같은 서지·작가 구성의 도서는 새 ID로 다시 등록할 수 있다")
    void canRegisterBibliographicTwinOfDeletedBook() {
        Author author = authorRepository.save(new Author(new AuthorName("현진건")));
        Book deleted = bookRepository.save(newBook(new BookTitle("운수 좋은 날"), new Publisher("자체 제작"), 1924, 1, null));
        authorBookRepository.save(new AuthorBook(author, deleted));
        bookRepository.delete(deleted.getId());
        BookUploadRequest request = new BookUploadRequest(new BookTitle("운수 좋은 날"), new Publisher("자체 제작"), 1924, null,
                List.of(new AuthorEntryRequest(author.getId(), null, null)),
                List.of(new ChapterUploadRequest(new ChapterTitle("1장"), List.of(passage("본문")))));

        BookUploadResponse response = bookIngestService.upload(request);

        assertThat(response.bookId()).isNotEqualTo(deleted.getId());
        assertThat(bookRepository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("작가 0명, 목차 0개, 본문 0개인 목차는 거부한다")
    void rejectEmptyStructures() {
        assertIngestFailure(() -> bookIngestService.upload(new BookUploadRequest(new BookTitle("제목"), null, null,
                        null, List.of(), chaptersWithOnePassage())), ErrorCode.INVALID_REQUEST,
                "작가는 최소 1명이어야 합니다.", "AUTHORS_EMPTY");
        assertIngestFailure(() -> bookIngestService.upload(new BookUploadRequest(new BookTitle("제목"), null, null,
                        null, authorsOfUnknown(), List.of())), ErrorCode.INVALID_REQUEST,
                "목차는 최소 1개여야 합니다.", "CHAPTERS_EMPTY");
        assertIngestFailure(() -> bookIngestService.upload(new BookUploadRequest(new BookTitle("제목"), null, null,
                        null, authorsOfUnknown(),
                        List.of(new ChapterUploadRequest(new ChapterTitle("1장"), List.of())))),
                ErrorCode.INVALID_REQUEST, "각 목차의 본문은 최소 1개여야 합니다.", "PASSAGES_EMPTY");
    }

    @Test
    @DisplayName("문장이 없거나 문장 내용이 공백이면 거부한다")
    void rejectBlankContent() {
        assertIngestFailure(() -> bookIngestService.upload(new BookUploadRequest(new BookTitle("제목"), null, null, null,
                authorsOfUnknown(), List.of(new ChapterUploadRequest(new ChapterTitle("1장"),
                        List.of(new PassageUploadRequest(List.of())))))), ErrorCode.INVALID_REQUEST,
                "각 문단의 문장은 최소 1개여야 합니다.", "SENTENCES_EMPTY");
        assertThatThrownBy(() -> bookIngestService.upload(new BookUploadRequest(new BookTitle("제목"), null, null, null,
                authorsOfUnknown(), List.of(new ChapterUploadRequest(new ChapterTitle("1장"),
                        List.of(passage(" ")))))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("문장 내용이 TEXT 저장 한도를 넘으면 거부한다")
    void rejectSentenceExceedingTextLimit() {
        BookUploadRequest request = new BookUploadRequest(new BookTitle("제목"), null, null, null,
                authorsOfUnknown(), List.of(new ChapterUploadRequest(new ChapterTitle("1장"),
                List.of(passage("a".repeat(65_536))))));

        assertIngestFailure(() -> bookIngestService.upload(request), ErrorCode.INVALID_REQUEST,
                "문장 하나는 65535바이트를 넘을 수 없습니다.", "SENTENCE_TOO_LARGE");
    }

    @Test
    @DisplayName("authorId와 name을 함께 주는 작가 항목은 거부한다")
    void rejectAmbiguousAuthorEntry() {
        Author existing = authorRepository.save(new Author(new AuthorName("현진건")));

        assertIngestFailure(() -> bookIngestService.upload(requestWithAuthors(
                        new AuthorEntryRequest(existing.getId(), new AuthorName("현진건"), null))),
                ErrorCode.INVALID_REQUEST,
                "작가 항목은 {name, isni?} 또는 {authorId} 중 한 형태여야 합니다.", "MIXED_AUTHOR_REFERENCE");
    }

    private BookUploadRequest requestWithAuthors(AuthorEntryRequest... authors) {
        return new BookUploadRequest(new BookTitle("새 책"), null, null, null, List.of(authors), chaptersWithOnePassage());
    }

    private void assertIngestFailure(org.assertj.core.api.ThrowableAssert.ThrowingCallable operation,
                                     ErrorCode code, String message, String reason) {
        assertThatThrownBy(operation).isInstanceOfSatisfying(BookIngestException.class, failure -> {
            assertThat(failure.getCode()).isEqualTo(code);
            assertThat(failure.getMessage()).isEqualTo(message);
            assertThat(failure.getLogContext()).containsEntry("reason", reason);
        });
    }

    private List<ChapterUploadRequest> chaptersWithOnePassage() {
        return List.of(new ChapterUploadRequest(new ChapterTitle("1장"), List.of(passage("본문"))));
    }

    private PassageUploadRequest passage(String... contents) {
        return new PassageUploadRequest(java.util.Arrays.stream(contents)
                .map(SentenceContent::new)
                .map(SentenceUploadRequest::new)
                .toList());
    }

    private List<AuthorEntryRequest> authorsOfUnknown() {
        return List.of(new AuthorEntryRequest(null, new AuthorName("작자 미상"), null));
    }
}
