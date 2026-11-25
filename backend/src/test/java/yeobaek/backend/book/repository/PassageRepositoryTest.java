package yeobaek.backend.book.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.Chapter;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.book.domain.vo.ChapterTitle;
import yeobaek.backend.book.domain.vo.SentenceContent;
import yeobaek.backend.support.IntegrationTest;

// 목차 범위 집계와 본문 범위의 도서 격리·정렬은 저장소의 사용자 정의 쿼리를 검증한다.
@Transactional
class PassageRepositoryTest extends IntegrationTest {

    @Autowired
    private BookManagementRepository bookRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private PassageRepository passageRepository;

    @Test
    @DisplayName("목차별 본문 범위는 같은 책의 문단만 집계한다")
    void chapterRanges() {
        Book book = book("대상 책");
        Chapter first = chapterRepository.save(new Chapter(book, new ChapterTitle("첫 장"), 1));
        Chapter second = chapterRepository.save(new Chapter(book, new ChapterTitle("둘째 장"), 2));
        passage(first, 3);
        passage(second, 4);
        passage(first, 1);
        Chapter unrelated = chapterRepository.save(new Chapter(book("다른 책"), new ChapterTitle("첫 장"), 1));
        passage(unrelated, 99);

        assertThat(passageRepository.findChapterRangesByBookId(book.getId()))
                .extracting(range -> tuple(range.getChapterId(), range.getStartSequence(), range.getEndSequence()))
                .containsExactlyInAnyOrder(tuple(first.getId(), 1, 3), tuple(second.getId(), 4, 4));
    }

    @Test
    @DisplayName("본문 범위는 양끝을 포함하고 도서별 전체 순서대로 반환한다")
    void inclusiveRangeInBookOrder() {
        Book book = book("대상 책");
        Chapter chapter = chapterRepository.save(new Chapter(book, new ChapterTitle("첫 장"), 1));
        passage(chapter, 3);
        passage(chapter, 1);
        passage(chapter, 2);
        Chapter unrelated = chapterRepository.save(new Chapter(book("다른 책"), new ChapterTitle("첫 장"), 1));
        passage(unrelated, 2);

        assertThat(passageRepository.findRangeByBookId(book.getId(), 2, 3))
                .extracting(passage -> passage.getSequence().value()).containsExactly(2, 3);
    }

    private Book book(String title) {
        return bookRepository.save(new Book(new BookTitle(title), null, null, 100, null, null, null, null, null));
    }

    private void passage(Chapter chapter, int sequence) {
        passageRepository.save(new Passage(chapter, sequence, List.of(new SentenceContent("본문"))));
    }
}
