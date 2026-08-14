package yeobaek.backend.book.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;
import yeobaek.backend.book.domain.Author;
import yeobaek.backend.book.domain.AuthorBook;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.Chapter;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.support.IntegrationTest;

class BookMappingTest extends IntegrationTest {

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

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("도서-목차-본문 구조를 저장하고 본문에서 도서까지 연관을 타고 조회할 수 있다")
    void bookChapterPassageMapping() {
        Book book = bookRepository.save(new Book("운수 좋은 날", "자체 제작", 1924, 2));
        Chapter chapter = chapterRepository.save(new Chapter(book, "1장", 1));
        Passage saved = passageRepository.save(new Passage(chapter, 1, "새침하게 흐린 품이 눈이 올 듯하더니..."));

        transactionTemplate.executeWithoutResult(status -> {
            Passage found = passageRepository.findById(saved.getId()).orElseThrow();

            assertThat(found.getSequence()).isEqualTo(1);
            assertThat(found.getContent()).isEqualTo("새침하게 흐린 품이 눈이 올 듯하더니...");
            assertThat(found.getChapter().getBook().getTitle()).isEqualTo("운수 좋은 날");
        });
    }

    @Test
    @DisplayName("공저를 위해 하나의 도서에 여러 작가를 매핑할 수 있다")
    void coAuthorMapping() {
        Book book = bookRepository.save(new Book("공저 도서", null, null, 0));
        Author first = authorRepository.save(new Author("작가1"));
        Author second = authorRepository.save(new Author("작가2"));
        authorBookRepository.save(new AuthorBook(first, book));
        authorBookRepository.save(new AuthorBook(second, book));

        transactionTemplate.executeWithoutResult(status ->
                assertThat(authorBookRepository.findAll())
                        .extracting(mapping -> mapping.getAuthor().getName())
                        .containsExactlyInAnyOrder("작가1", "작가2"));
    }
}
