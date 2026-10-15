package yeobaek.backend.publicroom.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.Chapter;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.book.domain.vo.ChapterTitle;
import yeobaek.backend.book.domain.vo.SentenceContent;
import yeobaek.backend.support.BadRequestException;
import yeobaek.backend.support.ErrorCode;

class PublicRoomTest {

    @Test
    @DisplayName("공개방 도서의 문단은 읽을 수 있다")
    void ensureCanReadPassageInBook() {
        Book book = book("공개방 도서");
        PublicRoom room = new PublicRoom(book);
        Passage passage = passage(book);

        assertThatCode(() -> room.ensureCanRead(passage)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("다른 도서의 문단은 공개방에서 읽을 수 없다")
    void rejectPassageFromOtherBook() {
        PublicRoom room = new PublicRoom(bookWithId(1L, "공개방 도서"));
        Passage otherPassage = passage(bookWithId(2L, "다른 도서"));

        assertThatThrownBy(() -> room.ensureCanRead(otherPassage))
                .isInstanceOf(BadRequestException.class)
                .extracting("code").isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("삭제된 도서의 문단은 공개방에서 읽을 수 없다")
    void rejectDeletedBook() {
        Book book = book("삭제된 도서");
        PublicRoom room = new PublicRoom(book);
        Passage passage = passage(book);
        book.delete();

        assertThatThrownBy(() -> room.ensureCanRead(passage))
                .isInstanceOf(BadRequestException.class)
                .extracting("code").isEqualTo(ErrorCode.BOOK_NOT_AVAILABLE);
    }

    private Book book(String title) {
        return new Book(new BookTitle(title), null, null, 1, null);
    }

    private Book bookWithId(Long id, String title) {
        Book book = book(title);
        ReflectionTestUtils.setField(book, "id", id);
        return book;
    }

    private Passage passage(Book book) {
        Chapter chapter = new Chapter(book, new ChapterTitle("1장"), 1);
        return new Passage(chapter, 1, List.of(new SentenceContent("문장")));
    }
}
