package yeobaek.backend.support;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import yeobaek.backend.book.domain.Author;
import yeobaek.backend.book.domain.AuthorBook;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.Chapter;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.repository.AuthorBookRepository;
import yeobaek.backend.book.repository.AuthorRepository;
import yeobaek.backend.book.repository.BookRepository;
import yeobaek.backend.book.repository.ChapterRepository;
import yeobaek.backend.book.repository.PassageRepository;

/**
 * 개발용 시드 데이터 투입기. local 프로파일에서만 동작하며 프로덕션에는 포함되지 않는다.
 * 실제 책 콘텐츠는 M7 인제스트로 투입하고, 그 전까지 개발·수동 확인에 이 시드를 쓴다.
 */
@Component
@Profile("local")
@RequiredArgsConstructor
public class DevDataSeeder implements CommandLineRunner {

    private static final int CHAPTER_COUNT = 3;
    private static final int PASSAGES_PER_CHAPTER = 10;

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final AuthorBookRepository authorBookRepository;
    private final ChapterRepository chapterRepository;
    private final PassageRepository passageRepository;

    @Override
    public void run(String... args) {
        if (bookRepository.count() > 0) {
            return;
        }
        seed();
    }

    public void seed() {
        List<String> paragraphs = paragraphs();
        Book book = bookRepository.save(new Book("운수 좋은 날", "자체 제작", 1924, paragraphs.size()));
        Author author = authorRepository.save(new Author("현진건"));
        authorBookRepository.save(new AuthorBook(author, book));

        int sequence = 1;
        for (int chapterIndex = 1; chapterIndex <= CHAPTER_COUNT; chapterIndex++) {
            Chapter chapter = chapterRepository.save(new Chapter(book, chapterIndex + "장", chapterIndex));
            for (int i = 0; i < PASSAGES_PER_CHAPTER; i++) {
                passageRepository.save(new Passage(chapter, sequence, paragraphs.get(sequence - 1)));
                sequence++;
            }
        }
    }

    private List<String> paragraphs() {
        List<String> paragraphs = new ArrayList<>();
        paragraphs.add("새침하게 흐린 품이 눈이 올 듯하더니 눈은 아니 오고 얼다가 만 비가 추적추적 내리었다.");
        for (int i = 2; i <= CHAPTER_COUNT * PASSAGES_PER_CHAPTER; i++) {
            paragraphs.add("(개발용 시드 문단 " + i + ") 실제 본문은 인제스트 파이프라인으로 투입된다. "
                    + "이 문단은 읽기 화면·진도·댓글 개발을 위한 자리 채움 텍스트다.");
        }
        return paragraphs;
    }
}
