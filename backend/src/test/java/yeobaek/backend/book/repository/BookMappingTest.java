package yeobaek.backend.book.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import yeobaek.backend.book.domain.Author;
import yeobaek.backend.book.domain.AuthorBook;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.BookStatus;
import yeobaek.backend.book.domain.Chapter;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.domain.vo.AuthorName;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.book.domain.vo.ChapterTitle;
import yeobaek.backend.book.domain.vo.ContentSequence;
import yeobaek.backend.book.domain.vo.Publisher;
import yeobaek.backend.book.domain.vo.SentenceContent;
import yeobaek.backend.support.IntegrationTest;

class BookMappingTest extends IntegrationTest {

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
    private SentenceRepository sentenceRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("도서-목차-문단-문장 구조를 저장하고 문장에서 도서까지 연관을 타고 조회할 수 있다")
    void bookChapterPassageMapping() {
        Book book = bookRepository.save(new Book(new BookTitle("운수 좋은 날"), new Publisher("자체 제작"), 1924, 2, null));
        Chapter chapter = chapterRepository.save(new Chapter(book, new ChapterTitle("1장"), 1));
        Passage saved = passageRepository.save(new Passage(chapter, 1, java.util.List.of(new SentenceContent("새침하게 흐린 품이 "), new SentenceContent("눈이 올 듯하더니..."))));

        transactionTemplate.executeWithoutResult(status -> {
            Passage found = passageRepository.findById(saved.getId()).orElseThrow();

            assertThat(found.getSequence()).isEqualTo(new ContentSequence(1));
            assertThat(found.getSentences()).extracting("sequence")
                    .containsExactly(new ContentSequence(1), new ContentSequence(2));
            assertThat(found.getSentences()).extracting("content")
                    .containsExactly("새침하게 흐린 품이 ", "눈이 올 듯하더니...");
            assertThat(found.getChapter().getBook().getTitle()).isEqualTo(new BookTitle("운수 좋은 날"));
            assertThat(found.getSentences().getFirst().belongsTo(book)).isTrue();
        });
    }

    @Test
    @DisplayName("nullable 값 객체를 저장한 뒤 다시 조회한다")
    void reloadsNullableValueObjects() {
        Author author = authorRepository.save(new Author(new AuthorName("작자 미상")));
        Book book = bookRepository.save(new Book(new BookTitle("출판사 미상"), null, null, 1, null));

        transactionTemplate.executeWithoutResult(status -> {
            Author foundAuthor = authorRepository.findById(author.getId()).orElseThrow();
            Book foundBook = bookRepository.findById(book.getId()).orElseThrow();

            assertThat(foundAuthor.getIsni()).isNull();
            assertThat(foundBook.getPublisher()).isNull();
        });
    }

    @Test
    @DisplayName("본문 범위 조회는 문단과 문장을 순서대로 fetch join한다")
    void fetchesOrderedSentencesWithPassages() {
        Book book = bookRepository.save(new Book(new BookTitle("범위 조회 도서"), null, null, 2, null));
        Chapter chapter = chapterRepository.save(new Chapter(book, new ChapterTitle("1장"), 1));
        passageRepository.save(new Passage(chapter, 2, List.of(new SentenceContent("둘째 문단."))));
        passageRepository.save(new Passage(chapter, 1, List.of(new SentenceContent("첫 문장. "), new SentenceContent("둘째 문장."))));

        List<Passage> passages = transactionTemplate.execute(
                status -> passageRepository.findRangeByBookId(book.getId(), 1, 2));

        assertThat(passages).extracting(Passage::getSequence)
                .containsExactly(new ContentSequence(1), new ContentSequence(2));
        assertThat(passages.getFirst().getSentences()).extracting("sequence", "content")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(new ContentSequence(1), "첫 문장. "),
                        org.assertj.core.groups.Tuple.tuple(new ContentSequence(2), "둘째 문장."));
    }

    @Test
    @DisplayName("같은 문단에 중복된 문장 순서를 저장할 수 없다")
    void rejectsDuplicateSentenceSequenceInPassage() {
        Book book = bookRepository.save(new Book(new BookTitle("문장 순서 도서"), null, null, 1, null));
        Chapter chapter = chapterRepository.save(new Chapter(book, new ChapterTitle("1장"), 1));
        Passage passage = new Passage(chapter, 1, List.of(new SentenceContent("첫 문장."), new SentenceContent("둘째 문장.")));
        ReflectionTestUtils.setField(passage.getSentences().get(1), "sequence", new ContentSequence(1));

        assertThatThrownBy(() -> passageRepository.saveAndFlush(passage))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("문단에서 제거한 문장은 고아로 남지 않고 삭제된다")
    void removesOrphanedSentence() {
        Book book = bookRepository.save(new Book(new BookTitle("문장 생명주기 도서"), null, null, 1, null));
        Chapter chapter = chapterRepository.save(new Chapter(book, new ChapterTitle("1장"), 1));
        Passage passage = passageRepository.save(new Passage(chapter, 1, List.of(new SentenceContent("남길 문장. "), new SentenceContent("삭제할 문장."))));
        Long removedSentenceId = passage.getSentences().get(1).getId();

        transactionTemplate.executeWithoutResult(status -> {
            Passage managedPassage = passageRepository.findById(passage.getId()).orElseThrow();
            Object managedSentences = ReflectionTestUtils.getField(managedPassage, "sentences");
            assertThat(managedSentences).isInstanceOf(List.class);
            ((List<?>) managedSentences).remove(1);
            passageRepository.flush();
        });

        assertThat(sentenceRepository.findById(removedSentenceId)).isEmpty();
    }

    @Test
    @DisplayName("공저를 위해 하나의 도서에 여러 작가를 매핑할 수 있다")
    void coAuthorMapping() {
        Book book = bookRepository.save(new Book(new BookTitle("공저 도서"), null, null, 1, null));
        Author first = authorRepository.save(new Author(new AuthorName("작가1")));
        Author second = authorRepository.save(new Author(new AuthorName("작가2")));
        authorBookRepository.save(new AuthorBook(first, book));
        authorBookRepository.save(new AuthorBook(second, book));

        transactionTemplate.executeWithoutResult(status ->
                assertThat(authorBookRepository.findAll())
                        .extracting(mapping -> mapping.getAuthor().getName())
                        .containsExactlyInAnyOrder(new AuthorName("작가1"), new AuthorName("작가2")));
    }

    @Test
    @DisplayName("도서의 DELETED 상태를 영속화한다")
    void persistsDeletedStatus() {
        Book book = bookRepository.save(new Book(new BookTitle("삭제될 도서"), null, null, 1, null));
        bookRepository.delete(book.getId());

        transactionTemplate.executeWithoutResult(status -> {
            Book found = bookRepository.findById(book.getId()).orElseThrow();

            assertThat(found.getStatus()).isEqualTo(BookStatus.DELETED);
        });
    }
}
